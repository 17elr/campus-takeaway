package cn.campus.takeaway.service;

import cn.campus.takeaway.mapper.AppMapper;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AddressService {
    public static final List<String> BUILDINGS =
            List.of("一号宿舍楼", "二号宿舍楼", "三号宿舍楼", "四号宿舍楼", "五号宿舍楼", "教学楼", "图书馆", "实训楼");
    private final AppMapper mapper;

    public AddressService(AppMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional
    public void create(long userId, String building, String room, String name, String phone) {
        validate(building, room, name, phone);
        lock(userId);
        mapper.insertAddress(userId, building, room.strip(), name.strip(), phone.strip(), false);
        ensureDefault(userId);
    }

    public void edit(
            long userId, long id, String building, String room, String name, String phone) {
        validate(building, room, name, phone);
        if (mapper.updateAddress(id, userId, building, room.strip(), name.strip(), phone.strip())
                != 1) throw new IllegalArgumentException("地址不存在或无权修改");
    }

    @Transactional
    public void setDefault(long userId, long id) {
        lock(userId);
        if (mapper.findAddress(id, userId) == null)
            throw new IllegalArgumentException("地址不存在或无权修改");
        mapper.defaultAddress(id, userId);
    }

    @Transactional
    public void delete(long userId, long id) {
        lock(userId);
        if (mapper.deleteAddress(id, userId) != 1) throw new IllegalArgumentException("地址不存在或无权删除");
        ensureDefault(userId);
    }

    private void ensureDefault(long userId) {
        var addresses = mapper.listAddresses(userId);
        if (!addresses.isEmpty()
                && addresses.stream().noneMatch(a -> Boolean.TRUE.equals(a.getIsDefault())))
            mapper.defaultAddress(addresses.get(0).getId(), userId);
    }

    private void lock(long userId) {
        if (mapper.lockUser(userId) == null) throw new IllegalArgumentException("账号不存在");
    }

    private void validate(String building, String room, String name, String phone) {
        if (!BUILDINGS.contains(building)) throw new IllegalArgumentException("仅支持列表中的校内楼栋");
        InputChecks.text(room, "房间或收货点", 40);
        InputChecks.text(name, "联系人", 40);
        InputChecks.phone(phone);
    }
}
