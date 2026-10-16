public class TimeUtil {

    // "08:15" -> 495 (minutes since midnight). Throws if the text is not a valid time.
    public static int parse(String text) {
        if (text == null || !text.trim().matches("\\d{1,2}:\\d{2}")) {
            throw new IllegalArgumentException("Time must look like HH:MM, for example 08:15");
        }
        String[] parts = text.trim().split(":");
        int h = Integer.parseInt(parts[0]);
        int m = Integer.parseInt(parts[1]);
        if (h > 23 || m > 59) {
            throw new IllegalArgumentException("Time is out of range (00:00 - 23:59)");
        }
        return h * 60 + m;
    }

    // 495 -> "08:15". Wraps around midnight, so -10 gives "23:50".
    public static String format(int minutes) {
        int t = ((minutes % 1440) + 1440) % 1440;
        return String.format("%02d:%02d", t / 60, t % 60);
    }
}