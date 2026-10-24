package cn.campus.takeaway.service;

import java.math.BigDecimal;

public final class InputChecks {
    private InputChecks() {}

    public static String text(String value, String label, int max) {
        if (value == null || value.isBlank() || value.strip().length() > max)
            throw new IllegalArgumentException(label + "不能为空，且最多 " + max + " 字");
        return value.strip();
    }

    public static String phone(String value) {
        if (value == null || !value.matches("1[3-9]\\d{9}"))
            throw new IllegalArgumentException("请填写有效的 11 位手机号码");
        return value;
    }

    public static void password(String value) {
        if (value == null
                || value.length() < 6
                || value.length() > 64
                || value.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
            throw new IllegalArgumentException("密码须为 6–64 位，且不超过 72 字节");
    }

    public static String username(String value) {
        if (value == null || !value.matches("[A-Za-z0-9_]{3,40}"))
            throw new IllegalArgumentException("用户名须为 3–40 位字母、数字或下划线");
        return value;
    }

    public static BigDecimal price(BigDecimal value) {
        if (value == null
                || value.signum() <= 0
                || value.compareTo(new BigDecimal("9999.99")) > 0
                || value.stripTrailingZeros().scale() > 2)
            throw new IllegalArgumentException("价格须大于 0、最多两位小数，且不超过 9999.99 元");
        return value.setScale(2);
    }
}
