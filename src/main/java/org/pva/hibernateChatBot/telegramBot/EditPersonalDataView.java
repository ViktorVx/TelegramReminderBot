package org.pva.hibernateChatBot.telegramBot;

import com.vdurmont.emoji.EmojiParser;
import org.pva.hibernateChatBot.constants.ConstantStorage;
import org.pva.hibernateChatBot.person.Person;
import org.telegram.abilitybots.api.sender.MessageSender;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import static java.lang.Math.toIntExact;

public class EditPersonalDataView {

    public static void replyToEditPersonalData(Update upd, Person person, MessageSender sender) {
        StringBuilder msg = new StringBuilder();
        msg.append(EmojiParser.parseToUnicode(":information_source: Персональная информация:\n"));
        msg.append(String.format("Фамилия: %s\n", person.getLastName() == null ? "-" : person.getLastName()));
        msg.append(String.format("Имя: %s\n", person.getFirstName() == null ? "-" : person.getFirstName()));
        msg.append(String.format("Отчество: %s\n", person.getMiddleName() == null ? "-" : person.getMiddleName()));

        if (upd.hasCallbackQuery()) {
            long message_id = upd.getCallbackQuery().getMessage().getMessageId();
            long chat_id = upd.getCallbackQuery().getMessage().getChatId();
            String inline_message_id = upd.getCallbackQuery().getInlineMessageId();

            EditMessageText new_message = new EditMessageText().
                    setChatId(chat_id).
                    setMessageId(toIntExact(message_id)).
                    setInlineMessageId(inline_message_id).
                    setText(msg.toString());
            new_message.setReplyMarkup(KeyboardFactory.getInfoPersonEditKeyboard());
            try {
                sender.execute(new_message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (upd.hasMessage()) {
            long chat_id = upd.getMessage().getChatId();
            SendMessage new_message = new SendMessage().
                    setChatId(chat_id).
                    setText(msg.toString());
            new_message.setReplyMarkup(KeyboardFactory.getInfoPersonEditKeyboard());
            try {
                sender.execute(new_message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void replyToEditLastName(long chatId, MessageSender sender) {
        String MESSAGE =
                ConstantStorage.EDIT_PERSON_LAST_NAME_MESSAGE;
        try {
            sender.execute(new SendMessage()
                    .setText(MESSAGE)
                    .setChatId(chatId).setReplyMarkup(KeyboardFactory.getForceReplyKeyboard()));

        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public static void replyToEditFirstName(long chatId, MessageSender sender) {
        String MESSAGE =
                ConstantStorage.EDIT_PERSON_FIRST_NAME_MESSAGE;
        try {
            sender.execute(new SendMessage()
                    .setText(MESSAGE)
                    .setChatId(chatId).setReplyMarkup(KeyboardFactory.getForceReplyKeyboard()));

        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public static void replyToEditMiddleName(long chatId, MessageSender sender) {
        String MESSAGE =
                ConstantStorage.EDIT_PERSON_MIDDLE_NAME_MESSAGE;
        try {
            sender.execute(new SendMessage()
                    .setText(MESSAGE)
                    .setChatId(chatId).setReplyMarkup(KeyboardFactory.getForceReplyKeyboard()));

        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public static void replyToEditInfoBackButton(Update upd, MessageSender sender) {
        if (upd.hasCallbackQuery()) {
            String MESSAGE = String.format("%s, заполните данные о себе!\n", upd.getCallbackQuery().getFrom().getUserName());
            long message_id = upd.getCallbackQuery().getMessage().getMessageId();
            long chat_id = upd.getCallbackQuery().getMessage().getChatId();
            String inline_message_id = upd.getCallbackQuery().getInlineMessageId();

            EditMessageText new_message = new EditMessageText().
                    setChatId(chat_id).
                    setMessageId(toIntExact(message_id)).
                    setInlineMessageId(inline_message_id).
                    setText(MESSAGE);
            new_message.setReplyMarkup(KeyboardFactory.getInfoEditKeyboard());
            try {
                sender.execute(new_message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
