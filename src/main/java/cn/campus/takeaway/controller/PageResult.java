package cn.campus.takeaway.controller;

import java.util.List;

public record PageResult<T>(List<T> records, long total, int page, int size) {
    public static void validate(int page, int size) {
        if (page < 1 || page > 1000000 || size < 1 || size > 100)
            throw new IllegalArgumentException("page 须大于 0，size 须在 1–100 之间");
    }
}
