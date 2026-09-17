import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class CopyAndLaunch {

    // Базовый путь к каталогам-источникам
    private static final String BASE_SOURCE =
            "d:\\Параметры соединений и пользователи в базах филиалов\\";

    // Подпапка для "дакорт"
    private static final String DAKORT_SUB = "_Дакорт 2026\\";
    // Подпапка для "партнер"
    private static final String PARTNER_SUB = "_КрымБЮ2026\\";

    // Каталоги-приёмники
    private static final String DEST_DBA  = "d:\\яПользователи\\";
    private static final String DEST_USR  = "d:\\яПользователи\\usrdef\\";

    // Имена файлов
    private static final String FILE_DBA  = "1cv7.dba";
    private static final String FILE_USR  = "users.usr";

    // Допустимые города для каждой системы
    private static final List<String> DAKORT_CITIES = Arrays.asList(
            "армавир", "евпатория", "краснодар", "севастополь",
            "симферополь", "солнечногорск", "солнечногорск3ПЛ",
            "тверь", "феодосия", "ялта", "мв"
    );

    private static final List<String> PARTNER_CITIES = Arrays.asList(
            "джанкой", "евпатория", "керчь",
            "севастополь", "феодосия", "ялта"
    );

    // Содержимое bat-файла
    private static final String[] BAT_LINES = {
        
        "start \"\" \"c:\\Program Files (x86)\\1Cv77\\BIN\\1cv7s.exe\" "
            + "CONFIG /Dd:\\яПользователи /nДубинскийАртем /p060620131"
    };

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Использование: java CopyAndLaunch <система> <город>");
            System.err.println("  система: дакорт | партнер");
            System.err.println("  город:  зависит от системы (см. список ниже)");
            System.err.println("Дакорт:  " + String.join(", ", DAKORT_CITIES));
            System.err.println("Партнер: " + String.join(", ", PARTNER_CITIES));
            return;
        }

        String system = args[0].toLowerCase(Locale.ROOT).trim();
        String city   = args[1].trim();

        // Определяем подпапку и проверяем город
        String subFolder;
        List<String> validCities;

        if (system.equals("дакорт")) {
            subFolder = DAKORT_SUB;
            validCities = DAKORT_CITIES;
        } else if (system.equals("партнер")) {
            subFolder = PARTNER_SUB;
            validCities = PARTNER_CITIES;
        } else {
            System.err.println("Неизвестная система: \"" + system + "\".");
            System.err.println("Допустимые значения: дакорт, партнер");
            return;
        }

        // Проверяем город (без учёта регистра, но сохраняем написание пользователя)
        boolean cityFound = false;
        for (String c : validCities) {
            if (c.toLowerCase(Locale.ROOT).equals(city.toLowerCase(Locale.ROOT))) {
                cityFound = true;
                break;
            }
        }
        if (!cityFound) {
            System.err.println("Город \"" + city + "\" не поддерживается для системы \"" + system + "\".");
            System.err.println("Допустимые города: " + String.join(", ", validCities));
            return;
        }

        // Формируем путь к источнику: первая буква города — заглавная, остальное как ввёл пользователь
        String cityCapitalized = capitalizeCity(city);
        String sourceDir = BASE_SOURCE + subFolder + cityCapitalized + "\\";

        System.out.println("Каталог-источник: " + sourceDir);

        Path source = Paths.get(sourceDir);
        if (!Files.isDirectory(source)) {
            System.err.println("Каталог-источник не найден: " + sourceDir);
            return;
        }

        try {
            copyFile(source.resolve(FILE_DBA), Paths.get(DEST_DBA, FILE_DBA));
            copyFile(source.resolve(FILE_USR), Paths.get(DEST_USR, FILE_USR));

            System.out.println("Файлы скопированы. Запускаю 1C...");
            runBat();

        } catch (IOException e) {
            System.err.println("Ошибка при копировании: " + e.getMessage());
        } catch (InterruptedException e) {
            System.err.println("Процесс был прерван.");
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Делает первую букву заглавной, остальное оставляет как есть.
     * "евпатория" → "Евпатория"
     * "солнечногорск3ПЛ" → "Солнечногорск3ПЛ"
     */
    private static String capitalizeCity(String city) {
        if (city == null || city.isEmpty()) return city;
        return city.substring(0, 1).toUpperCase(Locale.ROOT) + city.substring(1);
    }

    private static void copyFile(Path from, Path to) throws IOException {
        if (!Files.exists(from)) {
            System.err.println("Файл не найден в источнике: " + from);
            return;
        }
        if (to.getParent() != null) {
            Files.createDirectories(to.getParent());
        }
        Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("Скопирован: " + from + " → " + to);
    }

    private static void runBat() throws IOException, InterruptedException {
        Path bat = Files.createTempFile("launch1c_", ".bat");
        
        try (OutputStreamWriter osw = new OutputStreamWriter(
                new FileOutputStream(bat.toFile()), 
                "UTF-8")) {
            
            osw.write("@echo off\r\n");
            osw.write("chcp 65001 >nul\r\n");
            for (String line : BAT_LINES) {
                osw.write(line + "\r\n");
            }
        }


        ProcessBuilder pb = new ProcessBuilder("cmd", "/c", bat.toString());
        pb.inheritIO(); // Наследует консоль, чтобы вы видели вывод 1С
        Process proc = pb.start();
        proc.waitFor();

        Files.deleteIfExists(bat);
    }

}
