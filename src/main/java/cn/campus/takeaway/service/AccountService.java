package cn.campus.takeaway.service;

import cn.campus.takeaway.mapper.AppMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {
    private final AppMapper mapper;
    private final PasswordEncoder encoder;
    public AccountService(AppMapper mapper, PasswordEncoder encoder) { this.mapper=mapper; this.encoder=encoder; }
    public void updateProfile(long userId,String name,String phone){
        name=InputChecks.text(name,"姓名",40);phone=InputChecks.phone(phone);
        if(mapper.updateProfile(userId,name,phone)!=1)throw new IllegalStateException("账号不可用，请重新登录");
    }
    @Transactional
    public void changePassword(long userId,String username,String currentPassword,String newPassword,String confirmPassword){
        InputChecks.password(newPassword);
        if(!newPassword.equals(confirmPassword))throw new IllegalArgumentException("两次新密码输入不一致");
        if(mapper.lockUser(userId)==null)throw new IllegalStateException("账号不存在");
        var user=mapper.findUser(username);
        if(user==null || user.getId()!=userId || !user.isEnabled())throw new IllegalStateException("账号不可用，请重新登录");
        if(currentPassword==null || !encoder.matches(currentPassword,user.getPassword()))throw new IllegalArgumentException("当前密码不正确");
        if(encoder.matches(newPassword,user.getPassword()))throw new IllegalArgumentException("新密码不能与当前密码相同");
        if(mapper.updatePassword(userId,encoder.encode(newPassword))!=1)throw new IllegalStateException("账号不可用，请重新登录");
    }
    @Transactional
    public void create(String username, String password, String realName, String phone, String role) {
        InputChecks.username(username); InputChecks.password(password);
        realName=InputChecks.text(realName,"姓名",40); phone=InputChecks.phone(phone);
        if (!java.util.Set.of("STUDENT","MERCHANT","RIDER").contains(role)) throw new IllegalArgumentException("请选择学生、商家或骑手角色");
        if (mapper.findUser(username)!=null) throw new IllegalArgumentException("用户名已存在，请换一个用户名");
        mapper.insertUser(username,encoder.encode(password),realName,phone,role);
        if ("MERCHANT".equals(role)) mapper.insertShop(mapper.findUser(username).getId(),realName+"的店铺","欢迎光临，请先完善店铺信息",true);
    }
}
