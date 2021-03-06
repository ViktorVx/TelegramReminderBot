package org.pva.hibernateChatBot.telegramBot;

import com.vdurmont.emoji.EmojiParser;
import org.joda.time.DateTime;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.pva.hibernateChatBot.entity.enums.Gender;
import org.pva.hibernateChatBot.entity.person.Person;
import org.pva.hibernateChatBot.entity.reminder.simpleReminder.SimpleReminder;
import org.pva.hibernateChatBot.telegramBot.constants.ConstantStorage;
import org.pva.hibernateChatBot.telegramBot.keyboards.KeyboardFactory;
import org.pva.hibernateChatBot.telegramBot.utils.BotUtils;
import org.pva.hibernateChatBot.telegramBot.views.EditPersonRegisterDataView;
import org.pva.hibernateChatBot.telegramBot.views.EditPersonalDataView;
import org.pva.hibernateChatBot.telegramBot.views.EditReminderView;
import org.telegram.abilitybots.api.bot.AbilityBot;
import org.telegram.abilitybots.api.objects.Ability;
import org.telegram.abilitybots.api.objects.Flag;
import org.telegram.abilitybots.api.objects.Reply;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.telegram.abilitybots.api.objects.Locality.ALL;
import static org.telegram.abilitybots.api.objects.Privacy.PUBLIC;
import static org.telegram.abilitybots.api.util.AbilityUtils.getChatId;

public class Bot extends AbilityBot {

    private static final String BOT_TOKEN = System.getenv("TELEGRAM_TOKEN");
    private static final String BOT_USERNAME = "ReminderVxBot";
    private static final Integer BOT_CREATOR_ID = Integer.valueOf(System.getenv("BOT_CREATOR_ID"));
    private final Map<String, Map<String, String>> editReminderMap = db.getMap(ConstantStorage.DBNS_EDIT_REMINDER);
    private final Map<String, Long> currentReminderIdsMap = db.getMap(ConstantStorage.DBNS_CURRENT_REMINDER_IDS);

    private final Map<String, Person> personsMap = db.getMap(ConstantStorage.DBNS_PERSONS);
    private final Map<String, List<SimpleReminder>> remindersMap = db.getMap(ConstantStorage.DBNS_SIMPLE_REMINDERS);
    private final Map<String, Long> remindersLastIndexMap = db.getMap(ConstantStorage.DBNS_REMINDERS_LAST_INDEX);

    public Bot() {
        super(BOT_TOKEN, BOT_USERNAME);
    }

    @Override
    public int creatorId() {
        return BOT_CREATOR_ID;
    }

    //*** COMMAND ABILITIES ********************************************************************************************

    public Ability replyToStart() {
        return Ability
                .builder()
                .name("start")
                .info("Starts the bot!")
                .locality(ALL)
                .privacy(PUBLIC)
                .action(ctx -> {
                    User user = ctx.user();
                    String personId = ctx.user().getId().toString();
                    if (!personsMap.containsKey(personId)) {
                        Person person = new Person();
                        person.setUserId(Long.valueOf(personId));
                        person.setChatId(ctx.chatId());
                        person.setFirstName(user.getFirstName());
                        person.setLastName(user.getLastName());
                        person.setLogin(user.getUserName().concat("@").concat(String.valueOf(user.getId())));
                        personsMap.put(user.getId().toString(), person);
                        remindersLastIndexMap.put(personId, 0L);
                        db.commit();
                    }

                    String MESSAGE = String.format(ConstantStorage.MSG_START, ctx.user().getUserName());
                    try {
                        sender.execute(new SendMessage()
                                .setText(MESSAGE)
                                .setChatId(ctx.chatId()));
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }
                })
                .build();
    }

    public Ability replyToInfo() {
        return Ability
                .builder()
                .name("info")
                .info("Edit user's info!")
                .locality(ALL)
                .privacy(PUBLIC)
                .action(ctx -> {
                    String MESSAGE = String.format("%s, заполните данные о себе!\n", ctx.user().getUserName());
                    try {
                        sender.execute(new SendMessage()
                                .setText(MESSAGE)
                                .setChatId(ctx.chatId()).setReplyMarkup(KeyboardFactory.getInfoEditKeyboard()));
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }
                })
                .build();
    }

    public Ability replyToHelp() {
        return Ability
                .builder()
                .name("help")
                .info("Help me!!!")
                .locality(ALL)
                .privacy(PUBLIC)
                .action(ctx -> {
                    String MESSAGE =
                            "Доступны следующие команды:\n" +
                                    "/start - начало работы\n" +
                                    "/info - информация о пользователе\n" +
                                    "/help - помощь\n" +
                                    "/addsimplereminder - добавить простое напоминание\n" +
                                    "/viewreminders - показать напоминания";
                    try {
                        sender.execute(new SendMessage()
                                .setText(MESSAGE)
                                .setChatId(ctx.chatId()));
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }
                })
                .build();
    }

    public Ability replyToAddSimpleReminder() {
        return Ability
                .builder()
                .name("addsimplereminder")
                .info("Add simple reminder")
                .locality(ALL)
                .privacy(PUBLIC)
                .action(ctx -> {
                    String MESSAGE = ConstantStorage.MSG_EDIT_REMINDER_TEXT;
                    try {
                        sender.execute(new SendMessage()
                                .setText(MESSAGE)
                                .setChatId(ctx.chatId()).setReplyMarkup(KeyboardFactory.getForceReplyKeyboard()));
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }
                })
                .build();
    }

    public Ability replyToViewReminders() {
        return Ability
                .builder()
                .name("viewreminders")
                .info("View reminders list")
                .locality(ALL)
                .privacy(PUBLIC)
                .action(ctx -> {
                    String message = EmojiParser.parseToUnicode(":calendar: Список напоминаний (/addsimplereminder):\n");
                    List<SimpleReminder> reminderList = new ArrayList<>();
                    if (remindersMap.containsKey(ctx.user().getId().toString()))
                        reminderList = remindersMap.get(ctx.user().getId().toString()).stream().filter(c -> c.isComplete() == null || !c.isComplete()).collect(Collectors.toList());

                    reminderList.sort((o1, o2) -> o2.getRemindDate().compareTo(o1.getRemindDate()));
                    for (SimpleReminder reminder : reminderList) {
                        message = message.concat(String.format("/".concat(ConstantStorage.PREFIX_REMINDERS_LIST).concat("%d %s %s - %s\n"),
                                reminder.getId(),
                                new SimpleDateFormat(ConstantStorage.FORMAT_DATE).format(reminder.getRemindDate()),
                                new SimpleDateFormat(ConstantStorage.FORMAT_TIME).format(reminder.getRemindDate()),
                                reminder.getText()));
                    }
                    try {
                        sender.execute(new SendMessage()
                                .setText(message)
                                .setChatId(ctx.chatId()));
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }
                })
                .build();
    }

    //*** REPLIES TO BUTTONS AND MESSAGES ******************************************************************************

    public Reply replyToButtons() {
        Consumer<Update> action = upd -> {
            String personId = upd.getCallbackQuery().getFrom().getId().toString();
            Person person = personsMap.get(personId);
            List<SimpleReminder> simpleReminderList = remindersMap.get(personId);
            SimpleReminder simpleReminder;
            //****************************************************
            long chatId = getChatId(upd);
            switch (upd.getCallbackQuery().getData()) {
                case ConstantStorage.CBD_EDIT_PERSON_DATA:
                    EditPersonalDataView.replyToEditPersonalData(upd, person, sender);
                    break;
                case ConstantStorage.CBD_EDIT_REGISTER_DATA:
                    EditPersonRegisterDataView.replyToEditRegisterData(upd, person, sender);
                    break;
                case ConstantStorage.CBD_EDIT_LAST_NAME:
                    EditPersonalDataView.replyToEditLastName(chatId, sender);
                    break;
                case ConstantStorage.CBD_EDIT_FIRST_NAME:
                    EditPersonalDataView.replyToEditFirstName(chatId, sender);
                    break;
                case ConstantStorage.CBD_EDIT_MIDDLE_NAME:
                    EditPersonalDataView.replyToEditMiddleName(chatId, sender);
                    break;
                case ConstantStorage.CBD_EDIT_PERSONAL_DATA_BACK_BUTTON:
                    EditPersonalDataView.replyToEditInfoBackButton(upd, sender);
                    break;
                case ConstantStorage.CBD_EDIT_EMAIL:
                    EditPersonRegisterDataView.replyToEmail(chatId, sender);
                    break;
                case ConstantStorage.CBD_EDIT_BIRTH_DATE:
                    EditPersonRegisterDataView.replyToBirthDate(chatId, sender);
                    break;
                case ConstantStorage.CBD_EDIT_GENDER:
                    EditPersonRegisterDataView.replyToGender(chatId, upd, sender);
                    break;
                case ConstantStorage.CBD_EDIT_REGISTER_DATA_BACK_BUTTON:
                    EditPersonalDataView.replyToEditInfoBackButton(upd, sender);
                    break;
                case ConstantStorage.CBD_GENDER_SELECTOR_BACK_BUTTON:
                    EditPersonRegisterDataView.replyToRegisterBackButton(upd, person, sender);
                    break;
                case ConstantStorage.CBD_MALE_GENDER_SELECT:
                    person.setGender(Gender.MALE);
                    personsMap.put(personId, person);
                    EditPersonRegisterDataView.replyToEditRegisterData(upd, person, sender);
                    break;
                case ConstantStorage.CBD_FEMALE_GENDER_SELECT:
                    person.setGender(Gender.FEMALE);
                    personsMap.put(personId, person);
                    EditPersonRegisterDataView.replyToEditRegisterData(upd, person, sender);
                    break;
                case ConstantStorage.CBD_OTHER_GENDER_SELECT:
                    person.setGender(Gender.UNKNOWN);
                    personsMap.put(personId, person);
                    EditPersonRegisterDataView.replyToEditRegisterData(upd, person, sender);
                    break;
                case ConstantStorage.CBD_EDIT_REMINDER_TEXT:
                    simpleReminder = EditReminderView.getSimpleReminderFromMessage(simpleReminderList,
                            upd.getCallbackQuery().getMessage().getText());
                    if (simpleReminder == null) return;
                    currentReminderIdsMap.put(String.valueOf(upd.getCallbackQuery().getFrom().getId()), simpleReminder.getId());
                    EditReminderView.editNewReminderText(chatId, sender);
                    break;
                case ConstantStorage.CBD_EDIT_REMINDER_DATE:
                    simpleReminder = EditReminderView.getSimpleReminderFromMessage(simpleReminderList,
                            upd.getCallbackQuery().getMessage().getText());
                    if (simpleReminder == null) return;
                    currentReminderIdsMap.put(String.valueOf(upd.getCallbackQuery().getFrom().getId()), simpleReminder.getId());
                    EditReminderView.editNewReminderDate(chatId, sender);
                    break;
                case ConstantStorage.CBD_EDIT_REMINDER_TIME:
                    simpleReminder = EditReminderView.getSimpleReminderFromMessage(simpleReminderList,
                            upd.getCallbackQuery().getMessage().getText());
                    if (simpleReminder == null) return;
                    currentReminderIdsMap.put(String.valueOf(upd.getCallbackQuery().getFrom().getId()), simpleReminder.getId());
                    EditReminderView.editNewReminderTime(chatId, sender);
                    break;
                case ConstantStorage.CBD_EDIT_REMINDER_CLOSE:
                    simpleReminder = EditReminderView.getSimpleReminderFromMessage(simpleReminderList,
                            upd.getCallbackQuery().getMessage().getText());
                    if (simpleReminder != null) {
                        simpleReminder.setComplete(true);
                        remindersMap.put(personId, simpleReminderList);
                        EditReminderView.successCompleteReminder(chatId, simpleReminder, sender);
                    }
                    break;
                case ConstantStorage.CBD_EDIT_REMINDER_DELETE:
                    simpleReminder = EditReminderView.getSimpleReminderFromMessage(simpleReminderList,
                            upd.getCallbackQuery().getMessage().getText());
                    if (simpleReminder != null) {
                        simpleReminderList.remove(simpleReminder);
                        remindersMap.put(personId, simpleReminderList);
                        EditReminderView.successDeleteReminder(chatId, simpleReminder, sender);
                    }
                    break;
                case ConstantStorage.CBD_EDIT_REMINDER_BACK_BUTTON:
                    simpleReminderList = BotUtils.getActiveRemindersList(remindersMap.get(personId));
                    EditReminderView.viewRemindersList(upd, simpleReminderList, sender);
                    break;
                case ConstantStorage.CBD_EDIT_REMINDER_DELAY:
                    simpleReminder = EditReminderView.getSimpleReminderFromMessage(simpleReminderList,
                            upd.getCallbackQuery().getMessage().getText());
                    if (simpleReminder != null) {
                        EditReminderView.viewSelectReminder(simpleReminder, upd, sender);
                    }
                    break;
                case ConstantStorage.CBD_MAIN_MENU:
                    String MESSAGE = String.format(ConstantStorage.MSG_START,
                            upd.getCallbackQuery().getMessage().getFrom().getUserName());
                    try {
                        sender.execute(new EditMessageText()
                                .setText(MESSAGE).setMessageId(upd.getCallbackQuery().getMessage().getMessageId())
                                .setChatId(upd.getCallbackQuery().getMessage().getChat().getId()).setReplyMarkup(null));
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }
                    break;
            }
            //****************************************************
        };
        return Reply.of(action, Flag.CALLBACK_QUERY);
    }

    public Reply replyToMsg() {
        Consumer<Update> action = upd -> {
            String personId = upd.getMessage().getFrom().getId().toString();
            Person person = personsMap.get(personId);
            long userId = upd.getMessage().getFrom().getId();
            long chatId = upd.getMessage().getChatId();
            SendMessage sendMessage = new SendMessage();
            List<SimpleReminder> simpleReminderList;
            SimpleReminder simpleReminder;
            Date date;
            Date oldDate;
            //*********************************************************
            Message message = upd.getMessage();
            Message reply = upd.getMessage().getReplyToMessage();
            String msg = "";
            if (reply != null && reply.hasText()) {
                msg = reply.getText();
            }

            switch (msg) {
                case ConstantStorage.MSG_EDIT_PERSON_LAST_NAME:
                    person.setLastName(message.getText());
                    personsMap.put(personId, person);
                    db.commit();
                    EditPersonalDataView.replyToEditPersonalData(upd, person, sender);
                    break;
                case ConstantStorage.MSG_EDIT_PERSON_FIRST_NAME:
                    person.setFirstName(message.getText());
                    personsMap.put(personId, person);
                    EditPersonalDataView.replyToEditPersonalData(upd, person, sender);
                    break;
                case ConstantStorage.MSG_EDIT_PERSON_MIDDLE_NAME:
                    person.setMiddleName(message.getText());
                    personsMap.put(personId, person);
                    EditPersonalDataView.replyToEditPersonalData(upd, person, sender);
                    break;
                case ConstantStorage.MSG_EDIT_PERSON_EMAIL:
                    if (BotUtils.isValidEmail(message.getText())) {
                        person.setEmail(message.getText());
                    } else {
                        try {
                            sender.execute(new SendMessage().setChatId(chatId).
                                    setText(EmojiParser.parseToUnicode(ConstantStorage.ERR_MSG_WRONG_EMAIL_FORMAT)));
                        } catch (TelegramApiException e) {
                            e.printStackTrace();
                        }
                    }
                    personsMap.put(personId, person);
                    EditPersonRegisterDataView.replyToEditRegisterData(upd, person, sender);
                    break;
                case ConstantStorage.MSG_EDIT_PERSON_BIRTH_DATE:
                    Date birthDate = BotUtils.isValidDate(message.getText());
                    if (birthDate != null) {
                        person.setBirthDate(birthDate);
                    } else {
                        try {
                            sender.execute(new SendMessage().setChatId(chatId).
                                    setText(EmojiParser.parseToUnicode(ConstantStorage.ERR_MSG_WRONG_DATE_FORMAT)));
                        } catch (TelegramApiException e) {
                            e.printStackTrace();
                        }
                    }
                    personsMap.put(personId, person);
                    EditPersonRegisterDataView.replyToEditRegisterData(upd, person, sender);
                    break;
                case ConstantStorage.MSG_EDIT_REMINDER_TEXT:
                    String reminderText = message.getText();

                    Map<String, String> newRemMap = new HashMap<>();
                    newRemMap.put("text", reminderText);
                    editReminderMap.put(String.valueOf(userId), newRemMap);

                    try {
                        sender.execute(new SendMessage().setChatId(chatId).
                                setText(ConstantStorage.MSG_EDIT_REMINDER_DATE).
                                setReplyMarkup(KeyboardFactory.getForceReplyKeyboard()));
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }
                    break;
                case ConstantStorage.MSG_EDIT_REMINDER_DATE:
                    String reminderDateText = message.getText();
                    Date reminderDate = BotUtils.isValidDate(reminderDateText);
                    if (reminderDate == null) {
                        sendMessage.setChatId(chatId).
                                setText(EmojiParser.parseToUnicode(ConstantStorage.ERR_MSG_WRONG_DATE_FORMAT));
                    } else {

                        Map<String, String> remMap = editReminderMap.get(String.valueOf(userId));
                        remMap.put("date", new SimpleDateFormat(ConstantStorage.FORMAT_DATE).format(reminderDate));
                        editReminderMap.put(String.valueOf(userId), remMap);

                        sendMessage.setChatId(chatId).
                                setText(ConstantStorage.MSG_EDIT_REMINDER_TIME).
                                setReplyMarkup(KeyboardFactory.getForceReplyKeyboard());
                    }
                    try {
                        sender.execute(sendMessage);
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }
                    break;
                case ConstantStorage.MSG_EDIT_REMINDER_TIME:
                    String reminderTimeText = message.getText();
                    Map<String, Integer> reminderTime = BotUtils.isValidTime(reminderTimeText);
                    if (reminderTime == null) {
                        sendMessage.setChatId(chatId).
                                setText(EmojiParser.parseToUnicode(ConstantStorage.ERR_MSG_WRONG_TIME_FORMAT));
                    } else {
                        Map<String, String> remMap = editReminderMap.get(String.valueOf(userId));
                        simpleReminder = new SimpleReminder();
                        simpleReminder.setCreationDate(new Date());
                        simpleReminder.setText(remMap.get("text"));
                        simpleReminder.setComplete(false);

                        String stringDateTime = remMap.get("date").concat(" ").concat(reminderTimeText);
                        DateTimeFormatter dateTimeFormatter
                                = DateTimeFormat.forPattern("dd.MM.yyyy HH.mm");
                        DateTime remDateTime = DateTime.parse(stringDateTime, dateTimeFormatter);
                        simpleReminder.setRemindDate(remDateTime.toDate());

                        simpleReminder.setId(remindersLastIndexMap.get(personId) + 1);
                        remindersLastIndexMap.compute(personId, (k, ind) -> (ind == null ? 0 : ind) + 1);

                        if (!remindersMap.containsKey(personId)) {
                            simpleReminderList = new ArrayList<>();
                        } else {
                            simpleReminderList = remindersMap.get(personId);
                        }
                        simpleReminderList.add(simpleReminder);
                        remindersMap.put(personId, simpleReminderList);
                        db.commit();

                        sendMessage.setChatId(chatId).
                                setText(EmojiParser.parseToUnicode(ConstantStorage.MSG_SUCCESS_REMINDER_ADDITION));
                    }
                    try {
                        sender.execute(sendMessage);
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }
                    break;
                case ConstantStorage.MSG_EDIT_NEW_REMINDER_TEXT:
                    simpleReminder = person.getSimpleReminderById(currentReminderIdsMap.get(String.valueOf(upd.getMessage().getFrom().getId())),
                            remindersMap.get(personId));
                    simpleReminderList = remindersMap.get(String.valueOf(upd.getMessage().getFrom().getId()));
                    for (SimpleReminder reminder : simpleReminderList) {
                        if (simpleReminder.getId().equals(reminder.getId())) {
                            simpleReminder = reminder;
                            reminder.setText(upd.getMessage().getText());
                            break;
                        }
                    }
                    remindersMap.put(personId, simpleReminderList);
                    EditReminderView.viewSelectReminder(simpleReminder, upd, sender);
                    break;
                case ConstantStorage.MSG_EDIT_NEW_REMINDER_DATE:
                    simpleReminder = person.getSimpleReminderById(currentReminderIdsMap.get(String.valueOf(upd.getMessage().getFrom().getId())),
                            remindersMap.get(personId));
                    simpleReminderList = remindersMap.get(String.valueOf(upd.getMessage().getFrom().getId()));

                    date = BotUtils.isValidDate(upd.getMessage().getText());
                    oldDate = simpleReminder.getRemindDate();
                    if (date != null) {
                        LocalDateTime localDateTime = LocalDateTime.fromDateFields(date);
                        if (oldDate != null) {
                            LocalDateTime oldLocalDateTime = LocalDateTime.fromDateFields(oldDate);
                            localDateTime = new LocalDateTime(localDateTime.getYear(), localDateTime.getMonthOfYear(),
                                    localDateTime.getDayOfMonth(), oldLocalDateTime.getHourOfDay(), oldLocalDateTime.getMinuteOfHour());
                        }
                        for (SimpleReminder reminder : simpleReminderList) {
                            if (simpleReminder.getId().equals(reminder.getId())) {
                                simpleReminder = reminder;
                                reminder.setRemindDate(localDateTime.toDate());
                                break;
                            }
                        }
                    }
                    remindersMap.put(personId, simpleReminderList);
                    EditReminderView.viewSelectReminder(simpleReminder, upd, sender);
                    break;
                case ConstantStorage.MSG_EDIT_NEW_REMINDER_TIME:
                    simpleReminder = person.getSimpleReminderById(currentReminderIdsMap.get(String.valueOf(upd.getMessage().getFrom().getId())),
                            remindersMap.get(personId));
                    simpleReminderList = remindersMap.get(String.valueOf(upd.getMessage().getFrom().getId()));

                    Map<String, Integer> dateMap = BotUtils.isValidTime(upd.getMessage().getText());
                    oldDate = simpleReminder.getRemindDate();
                    if (dateMap != null) {
                        LocalDateTime localDateTime = LocalDateTime.fromDateFields(oldDate);
                        localDateTime = new LocalDateTime(localDateTime.getYear(), localDateTime.getMonthOfYear(),
                                localDateTime.getDayOfMonth(), dateMap.get("hours"), dateMap.get("minutes"));

                        for (SimpleReminder reminder : simpleReminderList) {
                            if (simpleReminder.getId().equals(reminder.getId())) {
                                simpleReminder = reminder;
                                reminder.setRemindDate(localDateTime.toDate());
                                break;
                            }
                        }
                    }

                    remindersMap.put(personId, simpleReminderList);
                    EditReminderView.viewSelectReminder(simpleReminder, upd, sender);
                    break;
            }
            //*********************************************************
        };
        return Reply.of(action, Flag.MESSAGE, Flag.REPLY);
    }

    public Reply replyToReminderSelection() {
        Consumer<Update> action = upd -> {
            String personId = upd.getMessage().getFrom().getId().toString();
            Person person = personsMap.get(personId);
            Long id = Long.valueOf(upd.getMessage().getText().replace("/".concat(ConstantStorage.PREFIX_REMINDERS_LIST), ""));
            SimpleReminder simpleReminder = person.getSimpleReminderById(id, remindersMap.get(personId));

            EditReminderView.viewSelectReminder(simpleReminder, upd, sender);
        };
        return Reply.of(action, Flag.MESSAGE, isReplyToReminderSelector());
    }

    //*** ADDITIONAL PREDICATES ****************************************************************************************

    private Predicate<Update> isReplyToMessage(String message) {
        return upd -> {
            Message reply = upd.getMessage().getReplyToMessage();
            return reply != null && reply.hasText() && reply.getText().equalsIgnoreCase(message);
        };
    }

    private Predicate<Update> isReplyToBot() {
        return upd -> upd.getMessage().getReplyToMessage().getFrom().getUserName().equalsIgnoreCase(getBotUsername());
    }

    private Predicate<Update> isReplyToReminderSelector() {
        return upd -> upd.getMessage().getText().matches("^/".concat(ConstantStorage.PREFIX_REMINDERS_LIST).concat("[0-9]+"));
    }

    //*** REMINDER SCHEDULED CREATION ***********************************************************************************

    public void mainScheduledTask(Integer horizonLength) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime horizon = now.plusMillis(horizonLength);

        for (String personId : remindersMap.keySet()) {
            for (SimpleReminder simpleReminder : remindersMap.get(personId)) {
                if (simpleReminder.isComplete()) continue;
                if (simpleReminder.getRemindDate().before(now.toDate())) continue;
                LocalDateTime remDateTime = LocalDateTime.fromDateFields(simpleReminder.getRemindDate());
                if (remDateTime.compareTo(horizon) <= 0) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(String.format("Напоминание /%s%s:\n",
                            ConstantStorage.PREFIX_REMINDERS_LIST,
                            simpleReminder.getId()));
                    stringBuilder.append(simpleReminder.getText());

                    long delay = simpleReminder.getRemindDate().getTime() - now.toDate().getTime();
                    Thread remThread = new Thread(() -> {
                        try {
                            Thread.sleep(delay <= 0 ? horizonLength : delay);
                            sender.execute(new SendMessage().
                                    setText(stringBuilder.toString()).
                                    setChatId(personsMap.get(personId).getUserId()).setReplyMarkup(KeyboardFactory.getCloseReminderKeyboard()));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    );
                    remThread.start();
                }
            }
        }
    }
    //******************************************************************************************************************
}
