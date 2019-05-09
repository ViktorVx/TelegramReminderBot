package org.pva.hibernateChatBot.telegramBot;

import com.vdurmont.emoji.EmojiParser;
import org.pva.hibernateChatBot.constants.ConstantStorage;
import org.pva.hibernateChatBot.person.Person;
import org.telegram.abilitybots.api.sender.MessageSender;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Math.toIntExact;

public class EditPersonRegisterDataView {

    private static final String EMAIL_PATTERN =
            "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@" +
                    "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
    private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy");

    public static void replyToEditRegisterData(Update upd, Person person, MessageSender sender) {
        StringBuilder msg = new StringBuilder();
        msg.append(EmojiParser.parseToUnicode(":floppy_disk: Регистрационные данные:\n"));
        msg.append(String.format("Email: %s\n", person.getEmail() == null ? "-" : person.getEmail()));
        msg.append(String.format("Дата рождения: %s\n", person.getBirthDate() == null ? "-" : new SimpleDateFormat("dd.MM.yyyy").format(person.getBirthDate())));

        String gender = "-";
        if (person.getGender() != null) {
            switch (person.getGender()) {
                case MALE:
                    gender = "Мужской";
                    break;
                case FEMALE:
                    gender = "Женский";
                    break;
                default:
                    gender = "Экзотический";
            }
        }
        msg.append(String.format("Пол: %s\n", gender));

        if (upd.hasCallbackQuery()) {
            long message_id = upd.getCallbackQuery().getMessage().getMessageId();
            long chat_id = upd.getCallbackQuery().getMessage().getChatId();
            String inline_message_id = upd.getCallbackQuery().getInlineMessageId();

            EditMessageText new_message = new EditMessageText().setChatId(chat_id).
                    setMessageId(toIntExact(message_id)).
                    setInlineMessageId(inline_message_id).
                    setText(msg.toString());
            new_message.setReplyMarkup(KeyboardFactory.getRegisterDataEditKeyboard());
            try {
                sender.execute(new_message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (upd.hasMessage()) {
            long chat_id = upd.getMessage().getChatId();
            SendMessage new_message = new SendMessage().setChatId(chat_id).setText(msg.toString());
            new_message.setReplyMarkup(KeyboardFactory.getRegisterDataEditKeyboard());
            try {
                sender.execute(new_message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void replyToEmail(long chatId, MessageSender sender) {
        String MESSAGE =
                ConstantStorage.EDIT_PERSON_EMAIL_MESSAGE;
        try {
            sender.execute(new SendMessage()
                    .setText(MESSAGE)
                    .setChatId(chatId).setReplyMarkup(KeyboardFactory.getForceReplyKeyboard()));

        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public static void replyToBirthDate(long chatId, MessageSender sender) {
        String MESSAGE =
                ConstantStorage.EDIT_PERSON_BIRTH_DATE_MESSAGE;
        try {
            sender.execute(new SendMessage()
                    .setText(MESSAGE)
                    .setChatId(chatId).setReplyMarkup(KeyboardFactory.getForceReplyKeyboard()));

        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public static void replyToGender(long chatId, Update upd, MessageSender sender) {
        if (upd.hasCallbackQuery()) {
            String MESSAGE = ConstantStorage.EDIT_PERSON_GENDER_MESSAGE;
            long message_id = upd.getCallbackQuery().getMessage().getMessageId();
            String inline_message_id = upd.getCallbackQuery().getInlineMessageId();

            try {
                sender.execute(new EditMessageText()
                        .setText(MESSAGE).setMessageId(toIntExact(message_id)).setInlineMessageId(inline_message_id)
                        .setChatId(chatId).setReplyMarkup(KeyboardFactory.getGenderSelectKeyboard()));

            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }

    }

    public static void replyToRegisterBackButton(Update upd, Person person, MessageSender sender) {
        if (upd.hasCallbackQuery()) {
            StringBuilder msg = new StringBuilder();
            msg.append(EmojiParser.parseToUnicode(":floppy_disk: Регистрационные данные:\n"));
            msg.append(String.format("Email: %s\n", person.getEmail() == null ? "-" : person.getEmail()));
            msg.append(String.format("Дата рождения: %s\n", person.getBirthDate() == null ? "-" : new SimpleDateFormat("dd.MM.yyyy").format(person.getBirthDate())));

            String gender = "-";
            if (person.getGender() != null) {
                switch (person.getGender()) {
                    case MALE:
                        gender = "Мужской";
                        break;
                    case FEMALE:
                        gender = "Женский";
                        break;
                    default:
                        gender = "Экзотический";
                }
            }
            msg.append(String.format("Пол: %s\n", gender));

            long message_id = upd.getCallbackQuery().getMessage().getMessageId();
            long chat_id = upd.getCallbackQuery().getMessage().getChatId();
            String inline_message_id = upd.getCallbackQuery().getInlineMessageId();

            EditMessageText new_message = new EditMessageText().
                    setChatId(chat_id).
                    setMessageId(toIntExact(message_id)).
                    setInlineMessageId(inline_message_id).
                    setText(msg.toString());
            new_message.setReplyMarkup(KeyboardFactory.getRegisterDataEditKeyboard());
            try {
                sender.execute(new_message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static Boolean isValidEmail(String email) {
        Pattern pattern = Pattern.compile(EMAIL_PATTERN);
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }

    public static Date isValidBirthDate(String birthDate) {
        Date date;
        try {
            date = SIMPLE_DATE_FORMAT.parse(birthDate);
        } catch (ParseException e) {
            return null;
        }
        return date;
    }

}
