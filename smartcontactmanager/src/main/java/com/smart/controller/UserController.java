package com.smart.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.smart.dao.ContactRepository;
import com.smart.dao.UserRepository;
import com.smart.entities.Contact;
import com.smart.entities.User;
import com.smart.helper.Message;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/user")
public class UserController {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ContactRepository contactRepository;

	@Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder;

	// Method for adding common data to response
	@ModelAttribute
	public void addCommonData(Model model, Principal principal) {
		String userName = principal.getName();
		System.out.println("Username: " + userName);
		// Get the user using username(email)
		User user = userRepository.getUserByUserName(userName);
		System.out.println("User Details: " + user);

		model.addAttribute("user", user);
	}

	@RequestMapping("index")
	public String dashboard(Model model, Principal principal) {
//		String userName = principal.getName();
//		System.out.println("Username: " + userName);
//		// Get the user using username(email)
//		User user = userRepository.getUserByUserName(userName);
//		System.out.println("User Details: " + user);
//
//		model.addAttribute("user", user);
		model.addAttribute("title", "User Dashboard");
		return "normal/user_dashboard";
	}

	// Open add contact handler
	@GetMapping("/add-contact")
	public String openAddContactForm(Model model) {

		model.addAttribute("title", "Add Contact");
		model.addAttribute("contact", new Contact());
		return "normal/add_contact_form";
	}

	// Processing add contact form
	@PostMapping("/process-contact")
	public String processContact(@ModelAttribute Contact contact, @RequestParam("profileImage") MultipartFile file,
			Principal principal, HttpSession session) {

		try {
			String name = principal.getName();
			User user = this.userRepository.getUserByUserName(name);

			contact.setUser(user);

			// Uploading Image
			if (file.isEmpty()) {
				// if file is empty
				System.out.println("File is empty");
				contact.setImage("contact.png");
			} else {
				contact.setImage(file.getOriginalFilename());

				File saveFile = new ClassPathResource("static/img").getFile();
				Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + file.getOriginalFilename());
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

				System.out.println("Image uploaded");
			}

			user.getContacts().add(contact);
			this.userRepository.save(user);

			System.out.println("Data: " + contact);
			System.out.println("Contact added to database");

			// Success Message
			session.setAttribute("message", new Message("Contact saved.", "success"));
		} catch (Exception e) {
			System.out.println("ERROR: " + e.getMessage());
			e.printStackTrace();
			// Error Message
			session.setAttribute("message", new Message("Something went wrong! Try Again", "danger"));
			// redirAttrs.addFlashAttribute("message", new Message("Something went wrong!
			// Try Again", "danger"));
		}
		return "normal/add_contact_form";
	}

	// show contacts handler
	// contacts per page=5[n]
	// current page=0[page]
	@GetMapping("/show-contacts/{page}")
	public String showContacts(@PathVariable("page") Integer page, Model m, Principal principal) {

		m.addAttribute("title", "View Contacts");

		String userName = principal.getName();
		User user = this.userRepository.getUserByUserName(userName);

		// Pageable object will have two variables-
		// currentPage- page
		// contacts per page- 5
		Pageable pageable = PageRequest.of(page, 5);

		Page<Contact> contacts = this.contactRepository.findContactsByUser(user.getId(), pageable);

		m.addAttribute("contacts", contacts);
		m.addAttribute("currentPage", page);
		m.addAttribute("totalPages", contacts.getTotalPages());

		return "normal/show_contacts";
	}

	// Showing specific contact details
	@GetMapping("/{cId}/contact")
	public String showContactDetail(@PathVariable("cId") Integer cId, Model model, Principal principal) {
		System.out.println("CID: " + cId);

		Optional<Contact> contactOptional = this.contactRepository.findById(cId);
		Contact contact = contactOptional.get();

		String userName = principal.getName();
		User user = this.userRepository.getUserByUserName(userName);

		if (user.getId() == contact.getUser().getId()) {
			model.addAttribute("contact", contact);
			model.addAttribute("title", contact.getName());
		}

		return "normal/contact_detail";
	}

	// Delete Contact Handler
	@GetMapping("/delete/{cid}")
	public String deleteContact(@PathVariable("cid") Integer cId, Model model, Principal principal,
			HttpSession session) {

//		Optional<Contact> contactOptional = this.contactRepository.findById(cId);
//		Contact contact = contactOptional.get();
//
//		contact.setUser(null);
//		this.contactRepository.delete(contact);

		this.contactRepository.deleteContactById(cId);
		session.setAttribute("message", new Message("Contact Deleted Successfully", "success"));

		return "redirect:/user/show-contacts/0";
	}

	// Open Update Form Handler
	@PostMapping("/update-contact/{cid}")
	public String updateForm(@PathVariable("cid") Integer cId, Model m) {

		m.addAttribute("title", "Update Contact");

		Contact contact = this.contactRepository.findById(cId).get();
		m.addAttribute("contact", contact);

		return "normal/update_form";
	}

	// Update Contact Handler
	@PostMapping("/process-update")
	public String updateHandler(@ModelAttribute Contact contact, @RequestParam("profileImage") MultipartFile file,
			Model m, HttpSession session, Principal principal) {

		try {

			// Old contact detail
			Contact oldContactDetail = this.contactRepository.findById(contact.getcId()).get();

			// Rewrite image
			if (!file.isEmpty()) {
				// Delete Old Photo
				File deleteFile = new ClassPathResource("static/img").getFile();
				File file1 = new File(deleteFile, oldContactDetail.getImage());
				file1.delete();

				// Update New Photo
				File saveFile = new ClassPathResource("static/img").getFile();
				Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + file.getOriginalFilename());
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

				contact.setImage(file.getOriginalFilename());
			} else {
				contact.setImage(oldContactDetail.getImage());
			}

			User user = this.userRepository.getUserByUserName(principal.getName());
			contact.setUser(user);

			System.out.println("ContactName- " + contact.getName());

			this.contactRepository.save(contact);

			session.setAttribute("message", new Message("Your contact is updated.", "success"));

		} catch (Exception e) {
			e.printStackTrace();
		}

		return "redirect:/user/" + contact.getcId() + "/contact";
	}

	// User Profile Handler
	@GetMapping("/profile")
	public String userProfile(Model model) {

		model.addAttribute("title", "Profile");
		return "normal/profile";
	}

	// Open Settings Handler
	@GetMapping("/settings")
	public String openSettings(Model model) {
		model.addAttribute("title", "Change Password");
		return "normal/settings";
	}

	// Change password handler
	@PostMapping("/change-password")
	public String changePassword(@RequestParam("oldPassword") String oldPassword,
			@RequestParam("newPassword") String newPassword, Principal principal, HttpSession session) {
		
		System.out.println("Old Password: " + oldPassword);
		System.out.println("New Password: " + newPassword);

		String userName = principal.getName();
		User currentUser = this.userRepository.getUserByUserName(userName);
		System.out.println("Password: " + currentUser.getPassword());

		if (this.bCryptPasswordEncoder.matches(oldPassword, currentUser.getPassword())) {
			
			if(oldPassword.equals(newPassword)) {
				session.setAttribute("message", new Message("Old & New passwords should not be same", "danger"));
				return "redirect:/user/settings";
			}
			else {
				currentUser.setPassword(this.bCryptPasswordEncoder.encode(newPassword));
				this.userRepository.save(currentUser);
				session.setAttribute("message", new Message("Your password is successfully changed", "success"));
				
			}
		}
		else {
			session.setAttribute("message", new Message("Please verify your old password!!!", "danger"));
			return "redirect:/user/settings";
		}

		return "redirect:/user/index";
	}

}
