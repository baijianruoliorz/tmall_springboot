/**
* 模仿天猫整站 springboot 教程 为 how2j.cn 版权所有
* 本教程仅用于学习使用，切勿用于非法用途，由此引起一切后果与本站无关
* 供购买者学习，请勿私自传播，否则自行承担相关法律责任
*/	

package com.how2java.tmall.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
@Controller
public class AdminPageController{
	@GetMapping(value = "/admin")
	public String admin(){
		return "redirect:admin_category_list";
	}
	@GetMapping(value = "/admin_category_list")
	public String listCategory(){
		return "admin/listCategory";
	}
	@GetMapping(value = "/admin_category_edit")
	public String editCategory(){
		return "admin/editCategory";
	}
}
