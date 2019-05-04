package org.pva.hibernateChatBot;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.pva.hibernateChatBot.enums.Gender;
import org.pva.hibernateChatBot.person.Person;
import org.pva.hibernateChatBot.person.PersonDao;
import org.pva.hibernateChatBot.person.PersonService;
import org.pva.hibernateChatBot.reminder.SimpleReminder;
import org.pva.hibernateChatBot.telegramBot.Bot;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;

import java.util.Date;
import java.util.List;

public class Main {
    private static final SessionFactory ourSessionFactory;
    private static final Integer RANDOM_FROM = 1;
    private static final Integer RANDOM_TO = 100000;
    private static PersonDao personDao;
    private static final String BOT_TOKEN = System.getenv("TELEGRAM_TOKEN");
    private static final String BOT_USERNAME = "ReminderVxBot";

    static {
        try {
            Configuration configuration = new Configuration();
            configuration.configure();

            ourSessionFactory = configuration.buildSessionFactory();
        } catch (Throwable ex) {
            throw new ExceptionInInitializerError(ex);
        }
        personDao = new PersonDao(ourSessionFactory);
    }

    public static Session getSession() throws HibernateException {
        return ourSessionFactory.openSession();
    }

    public static void main(final String[] args) throws Exception {


        ApiContextInitializer.init();
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi();
        try {
            telegramBotsApi.registerBot(new Bot());
        } catch (TelegramApiRequestException e) {
            e.printStackTrace();
        }

        while (true) {

        }
    }

    private static void getFromDatabase() {
        List<Person> personList = personDao.getAll();
        Person singlePerson = personDao.get(2).orElse(null);
        for (Person person : personList) {
            System.out.println(PersonService.getFullName(person));
        }
        System.out.println("**************************************");
        System.out.println(PersonService.getFullName(singlePerson));
    }


    private static void fulfillDatabase() {
//        PersonDao personDao = new PersonDao(ourSessionFactory);
        Person person = new Person(getRandom("Иванов"), getRandom("Иван"), getRandom("Иванович"));
        person.setLogin(getRandom("login"));
        person.setBirthDate(new Date());
        person.setGender(Gender.MALE);
        SimpleReminder simpleReminder = new SimpleReminder(person, new Date(), getRandom("Простое напоминание"), new Date());
        person.getReminderList().add(simpleReminder);
        personDao.save(person);

        person = new Person(getRandom("Петров"), getRandom("Петр"), getRandom("Петрович"));
        person.setLogin(getRandom("login"));
        person.setBirthDate(new Date());
        person.setGender(Gender.MALE);
        simpleReminder = new SimpleReminder(person, new Date(), getRandom("Простое напоминание"), new Date());
        person.getReminderList().add(simpleReminder);
        personDao.save(person);

        person = new Person(getRandom("Соколова"), getRandom("Наталья"), getRandom("Павловна"));
        person.setLogin(getRandom("login"));
        person.setBirthDate(new Date());
        person.setGender(Gender.FEMALE);
        simpleReminder = new SimpleReminder(person, new Date(), getRandom("Простое напоминание"), new Date());
        person.getReminderList().add(simpleReminder);
        simpleReminder = new SimpleReminder(person, new Date(), getRandom("Простое напоминание"), new Date());
        person.getReminderList().add(simpleReminder);
        personDao.save(person);
    }

    private static String getRandom(String str) {
        return str.concat("(").concat(String.valueOf(RANDOM_FROM + (int) (Math.random() * RANDOM_TO))).concat(")");
    }

}