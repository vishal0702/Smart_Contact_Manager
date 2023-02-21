package com.smart.controller;

import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.smart.dao.UserRepository;
import com.smart.entities.User;
import com.smart.service.EmailService;

import jakarta.servlet.http.HttpSession;

@Controller
public class ForgotController {
	
	@Autowired
	private EmailService emailService;
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder;

	// Random number
	Random random = new Random(1000);
	

	// emailid form open handler
	@RequestMapping("/forgot-password")
	public String openEmailForm(Model model) {
		
		model.addAttribute("title", "Forgot Password-Smart Contact Manager");
		return "forgot_email_form";
	}

	@PostMapping("/send-otp")
	public String sendOtp(@RequestParam("email") String email,Model model, HttpSession session) {
		
		model.addAttribute("title", "Verify OTP-Smart Contact Manager");

		System.out.println("Email: " + email);
		model.addAttribute("email", email);

		// Generating 4 digit OTP

		int otp = random.nextInt(9999);
		System.out.println("OTP: " + otp);
		
		//Code to send OTP to email
		
		User user = this.userRepository.getUserByUserName(email);
		if(user==null) {
			//send error message
			session.setAttribute("message", "Provided Mail Id is not registered with us!");
			return "forgot_email_form";
		}else {
			String subject="OTP from Smart Contact Manager";
			String message=""
					+ "<div style='border:1px solid #e2e2e2; padding:20px'>"
					+ "<h1>"
					+ "OTP : "
					+ "<b>"+otp
					+ "</b>"
					+ "</h1>"
					+ "</div>";
			String to=email;
			String from="vishalsrivi123@gmail.com";
			
			boolean flag = this.emailService.sendEmail(to, from, subject, message);
			if(flag) {
				session.setAttribute("myotp", otp);
				session.setAttribute("email", email);
				return "verify_otp";
			}else {
				session.setAttribute("message", "Please verify your email address!");
				return "forgot_email_form";
			}
		}
	}
	
	
	//verify otp
	@PostMapping("/verify-otp")
	public String verifyOtp(@RequestParam("otp") int otp, HttpSession session) {
		
		int myOtp=(int) session.getAttribute("myotp");
		String email=(String) session.getAttribute("email");
		
		if(myOtp==otp) {
			
			User user = this.userRepository.getUserByUserName(email);
			if(user==null) {
				//send error message
				session.setAttribute("message", "Provided Mail Id is not registered with us!");
				return "forgot_email_form";
			}else {
				//send change password form
			}
			
			return "password_change_form";
		}else {
			session.setAttribute("message", "Invalid OTP");
			return "verify_otp";
		}
	}
	
	
	//Change Password
	@PostMapping("/change-password")
	public String changePassword(@RequestParam("newpassword") String newpassword,HttpSession session) {
		
		String email = (String) session.getAttribute("email");
		User user = this.userRepository.getUserByUserName(email);
		user.setPassword(this.bCryptPasswordEncoder.encode(newpassword));
		this.userRepository.save(user);
		
		return "redirect:/signin?change=Password Changed Successfully.";
	}
}
