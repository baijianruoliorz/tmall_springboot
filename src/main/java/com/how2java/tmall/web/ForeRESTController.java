package com.how2java.tmall.web;

import com.how2java.tmall.comparator.*;
import com.how2java.tmall.pojo.*;
import com.how2java.tmall.service.*;
import com.how2java.tmall.util.Result;
import org.apache.commons.lang.math.RandomUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.crypto.SecureRandomNumberGenerator;
import org.apache.shiro.crypto.hash.SimpleHash;
import org.apache.shiro.subject.Subject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.HtmlUtils;

import javax.servlet.http.HttpSession;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class ForeRESTController {
    @Autowired
    CategoryService categoryService;
    @Autowired
    OrderService orderService;
    @Autowired
    ProductService productService;
    @Autowired
    UserService userService;
    @Autowired
    ProductImageService productImageService;
    @Autowired
    PropertyValueService propertyValueService;
    @Autowired
    OrderItemService orderItemService;
    @Autowired
    ReviewService reviewService;

    @GetMapping("/forehome")
    public Object home() {
        List<Category> cs = categoryService.list();
        productService.fill(cs);
        productService.fillByRow(cs);
        categoryService.removeCategoryFromProduct(cs);

        return cs;
    }

    //    @PostMapping("/foreregister")
//    public Object register(@RequestBody User user){
//        String name=user.getName();
//        String password=user.getPassword();
//        name=HtmlUtils.htmlEscape(name);
//        user.setName(name);
//        boolean exist=userService.isExist(name);
//        if(exist){
//            String message="用户名已经使用，不能在注册";
//            return Result.fail(message);
//        }
//        String salt=new SecureRandomNumberGenerator().nextBytes().toString();
//        int times=2;
//        String algorithmName="md5";
//
//        String encodedPassword=new SimpleHash(algorithmName,password,salt,times).toString();
//        user.setSalt(salt);
//        user.setPassword(encodedPassword);
//        userService.add(user);
//        return Result.success();
//    }
    @PostMapping("/foreregister")
    public Object register(@RequestBody User user) {
        String name = user.getName();
        String password = user.getPassword();
        name = HtmlUtils.htmlEscape(name);
        user.setName(name);
        boolean exist = userService.isExist(name);
        if (exist) {
            String message = "用户民已经使用,不能再次注册";
            return Result.fail(message);
        }
        String salt = new SecureRandomNumberGenerator().nextBytes().toString();
        int times = 2;
        String algorithmName = "md5";
        String encodedPassword = new SimpleHash(algorithmName, password, salt, times).toString();
        user.setSalt(salt);
        user.setPassword(encodedPassword);
        userService.add(user);
        return Result.success();

    }

    @GetMapping("/forelogout")
    public String logout() {
        Subject subject = SecurityUtils.getSubject();
        if (subject.isAuthenticated()) {
            subject.logout();
        }
        return "redirect:home";
    }

    @PostMapping("/forelogin")
    public Object login(@RequestBody User userParam, HttpSession session) {
        String name = userParam.getName();
        name = HtmlUtils.htmlEscape(name);

        Subject subject = SecurityUtils.getSubject();
        UsernamePasswordToken token = new UsernamePasswordToken(name, userParam.getPassword());
        try {
            subject.login(token);
            User user = userService.getByName(name);
//          subject.getSession().setAttribute("user", user);
            session.setAttribute("user", user);
            return Result.success();
        } catch (AuthenticationException e) {
            String message = "账号密码错误";
            return Result.fail(message);
        }

    }

    @GetMapping("/foreproduct/{pid}")
    public Object product(@PathVariable("pid") int pid) {
        Product product = productService.get(pid);

        List<ProductImage> productSingleImages = productImageService.listSingleProductImages(product);
        List<ProductImage> productDetailImages = productImageService.listDetailProductImages(product);
        product.setProductSingleImages(productSingleImages);
        product.setProductDetailImages(productDetailImages);

        List<PropertyValue> pvs = propertyValueService.list(product);
        List<Review> reviews = reviewService.list(product);
        productService.setSaleAndReviewNumber(product);
        productImageService.setFirstProdutImage(product);

        Map<String, Object> map = new HashMap<>();
        map.put("product", product);
        map.put("pvs", pvs);
        map.put("reviews", reviews);

        return Result.success(map);
    }

    @GetMapping("forecheckLogin")
    public Object checkLogin() {
        Subject subject = SecurityUtils.getSubject();
        if (subject.isAuthenticated()) {
            return Result.success();
        } else {
            return Result.fail("未登录");
        }
    }

    @GetMapping("forecategory/{cid}")
    public Object category(@PathVariable int cid, String sort) {
        Category c = categoryService.get(cid);
        productService.fill(c);
        productService.setSaleAndReviewNumber(c.getProducts());
        categoryService.removeCategoryFromProduct(c);

        if (null != sort) {
            switch (sort) {
                case "review":
                    Collections.sort(c.getProducts(), new ProductReviewComparator());
                    break;
                case "date":
                    Collections.sort(c.getProducts(), new ProductDateComparator());
                    break;

                case "saleCount":
                    Collections.sort(c.getProducts(), new ProductSaleCountComparator());
                    break;

                case "price":
                    Collections.sort(c.getProducts(), new ProductPriceComparator());
                    break;

                case "all":
                    Collections.sort(c.getProducts(), new ProductAllComparator());
                    break;
            }
        }

        return c;
    }

    //    @PostMapping("foresearch")
//    public Object search(String keyword) {
//        if (null == keyword) {
//            keyword = "";
//        }
//        List<Product> ps = productService.search(keyword, 0, 20);
//        productImageService.setFirstProductImages(ps);
//        productService.setSaleAndReviewNumber(ps);
//        return ps;
//    }
    @PostMapping("foresearch")
    public Object search(String keyword) {
            if (keyword == null) {
                keyword = "";
            }
            List<Product> ps = productService.search(keyword, 0, 20);
            productImageService.setFirstProductImages(ps);
            productService.setSaleAndReviewNumber(ps);
            return ps;
        }


    @GetMapping("forebuyone")
    public Object buyone(int pid, int num, HttpSession session) {
        return buyoneAndAddCart(pid, num, session);
    }

    private int buyoneAndAddCart(int pid, int num, HttpSession session) {
        Product product = productService.get(pid);
        int oiid = 0;

        User user = (User) session.getAttribute("user");
        boolean found = false;
        List<OrderItem> ois = orderItemService.listByUser(user);
        for (OrderItem oi : ois) {
            if (oi.getProduct().getId() == product.getId()) {
                oi.setNumber(oi.getNumber() + num);
                orderItemService.update(oi);
                found = true;
                oiid = oi.getId();
                break;
            }
        }
        if (!found) {
            OrderItem oi = new OrderItem();
            oi.setUser(user);
            oi.setProduct(product);
            oi.setNumber(num);
            orderItemService.add(oi);
            oiid = oi.getId();
        }
        return oiid;
    }

    @GetMapping("forebuy")
    public Object buy(String[] oiid, HttpSession session) {
        List<OrderItem> orderItems = new ArrayList<>();
        float total = 0;
        for (String strid : oiid) {
            int id = Integer.parseInt(strid);
            OrderItem oi = orderItemService.get(id);
            total += oi.getProduct().getPromotePrice() * oi.getNumber();
            orderItems.add(oi);
        }
        productImageService.setFirstProductImagesOnorderItems(orderItems);
        session.setAttribute("ois", orderItems);
        Map<String, Object> map = new HashMap<>();
        map.put("orderItems", orderItems);
        map.put("total", total);
        return Result.success(map);
    }

    @GetMapping("foreaddCart")
    public Object addCart(int pid, int num, HttpSession session) {
        buyoneAndAddCart(pid, num, session);
        return Result.success();
    }

    @GetMapping("forecart")
    public Object cart(HttpSession session) {
        User user = (User) session.getAttribute("user");
        List<OrderItem> ois = orderItemService.listByUser(user);
        productImageService.setFirstProductImagesOnorderItems(ois);
        return ois;
    }

    @GetMapping("forechangeOrderItem")
    public Object changeOrderItem(HttpSession session, int pid, int num) {
        User user = (User) session.getAttribute("user");
        if (null == user) {
            return Result.fail("未登录");
        }
        List<OrderItem> ois = orderItemService.listByUser(user);
        for (OrderItem oi : ois) {
            if (oi.getProduct().getId() == pid) {
                oi.setNumber(num);
                orderItemService.update(oi);
                break;
            }

        }
        return Result.success();
    }
//    @GetMapping("forechangeOrderItem")
//    public Object changeOrderItem(HttpSession session,int pid,int num){
//        Object user1 = session.getAttribute("user");
//        User user= (User) user1;
//        if (null==user){
//            return Result.fail("未登录");
//        }
//        List<OrderItem> ois=orderItemService.listByOrder(user);
//        for (OrderItem oi : ois) {
//            if (oi.getProduct().getId()==pid){
//                oi.setNumber(num);
//                orderItemService.update(oi);
//                break;
//            }
//        }
//        return Result.success();
//    }

    @GetMapping("foredeleteOrderItem")
    public Object deleteOrderItem(HttpSession session, int oiid) {
        User user = (User) session.getAttribute("user");
        if (null == user) {
            return Result.fail("未登录");
        }
        orderItemService.delete(oiid);
        return Result.success();
    }

    //    @PostMapping("forecreateOrder")
//    public Object createOrder(@RequestBody Order order, HttpSession session) {
//        User user = (User) session.getAttribute("user");
//        if (null == user) {
//            return Result.fail("未登录");
//        }
//        String orderCode = new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date()) + RandomUtils.nextInt(10000);
//        order.setOrderCode(orderCode);
//        order.setCreateDate(new Date());
//        order.setUser(user);
//        order.setStatus(OrderService.waitPay);
//        List<OrderItem> ois = (List<OrderItem>) session.getAttribute("ois");
//        float total = orderService.add(order, ois);
//        Map<String, Object> map = new HashMap<>();
//        map.put("oid", order.getId());
//        map.put("total", total);
//        return Result.success(map);
//    }
    @PostMapping("forecreateOrder")
    public Object createOrder(@RequestBody Order order, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (null == user) {
            return Result.fail("未登录");
        }
        String orderCode = new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date()) + RandomUtils.nextInt(10000);
        order.setOrderCode(orderCode);
        order.setCreateDate(new Date());
        order.setUser(user);
        order.setStatus(OrderService.waitPay);
        Object ois1 = session.getAttribute("ois");
        List<OrderItem> ois = (List<OrderItem>) ois1;
        float total = orderService.add(order, ois);
        Map<String, Object> map = new HashMap<>();
        map.put("oid", order.getId());
        map.put("total", total);
        return Result.success(map);

    }
}
