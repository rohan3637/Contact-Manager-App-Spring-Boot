package com.smart.contactmanager.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.Optional;

import javax.servlet.http.HttpSession;

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

import com.smart.contactmanager.dao.ContactRepository;
import com.smart.contactmanager.dao.UserRepository;
import com.smart.contactmanager.entities.Contact;
import com.smart.contactmanager.entities.User;
import com.smart.contactmanager.helper.Message;

@Controller
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ContactRepository contactRepository; 

    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @ModelAttribute
    public void addCommonData(Model model, Principal principal){
        String userName=principal.getName();
        System.out.println("User: "+ userName);
        //get the user using username(email)
        User user=userRepository.getUserByUserName(userName);
        model.addAttribute("user", user);
    }
    
    @RequestMapping("/index")
    public String dashboard(Model model, Principal principal){
        model.addAttribute("title", "User Dashboard");
        return "normal/user_dashboard";
    }

    //open add-contact form handler
    @GetMapping("/add-contact")
    public String addContactForm(Model model){
        model.addAttribute("title", "Add Contact");
        model.addAttribute("contact", new Contact());
        return "normal/add_contact_form";
    }

    //processing add contact form
    @PostMapping("/process-contact")
    public String processContact(@ModelAttribute Contact contact, @RequestParam("profileImage") MultipartFile multipartFile, Model model, Principal principal, HttpSession session){
        try {
            String name=principal.getName();
            User currUser=userRepository.getUserByUserName(name);
            if(multipartFile.isEmpty()){
                System.out.println("No file uploaded !!");
                contact.setImageUrl("contact_profile.png");
            }
            else{
                contact.setImageUrl(multipartFile.getOriginalFilename());
                File saveFile=new ClassPathResource("static/img").getFile();
                Path path=Paths.get(saveFile.getAbsolutePath()+File.separator+multipartFile.getOriginalFilename());
                Files.copy(multipartFile.getInputStream(),path, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Image Uploaded Successfully!!"); 
            }
            contact.setUser(currUser);
            currUser.getContacts().add(contact);
            this.userRepository.save(currUser);
            System.out.println("Contacts added successfully");

            //message success
            session.setAttribute("message", new Message("Contact added successfully !! Add more ....", "success")); 
        } catch (Exception e) {
            System.out.println("error: "+e.getMessage());
            e.printStackTrace();

            //message error
            session.setAttribute("message", new Message("Something went wrong !! Try Again ....", "danger"));
        }
        return "normal/add_contact_form";
    }

    //show contacts handler
    @GetMapping("/show-contacts/{page}")
    public String showContacts(@PathVariable("page") Integer page, Model model, Principal principal){
        model.addAttribute("title", "Show Contacts");
        String userName=principal.getName();    
        System.out.println(userName);
        User currUser=this.userRepository.getUserByUserName(userName);
        Pageable pageable=PageRequest.of(page,5);
        Page<Contact> contacts=contactRepository.getContactsByUser(currUser.getId(), pageable);
        model.addAttribute("contacts", contacts);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", contacts.getTotalPages());
        return "normal/show_contacts";
    }

    //showing particular contact details
    @GetMapping("/{cid}/contact")
    public String showContactDetail(@PathVariable("cid") Integer cid, Model model, Principal principal){
        Optional<Contact> contactOptional=this.contactRepository.findById(cid);
        Contact contact=contactOptional.get();
        String userName=principal.getName();
        User user=this.userRepository.getUserByUserName(userName);
        if(user.getId()==contact.getUser().getId()){
            model.addAttribute("contact", contact);
            model.addAttribute("title", contact.getName());
        }
        System.out.println("CId: "+cid);
        return "normal/contact-detail";
    }

    //deleting users contact
    @GetMapping("/delete/{cid}")
    public String deleteContact(@PathVariable("cid") Integer cid,Model model,Principal principal, HttpSession session){
        Optional<Contact> contactOptional=this.contactRepository.findById(cid);
        Contact contact=contactOptional.get();
        String userName=principal.getName();
        User user=this.userRepository.getUserByUserName(userName);
        if(user.getId()==contact.getUser().getId()){
            user.getContacts().remove(contact);
            this.userRepository.save(user);
            //contact.setUser(null);
            //this.contactRepository.delete(contact);
            session.setAttribute("message", new Message("Contact deleted successfully !!", "success"));
        }
        else{
            System.out.println("Access denied !!");
            session.setAttribute("message", new Message("Something went wrong !! Try Again ....", "danger"));
        }
        return "redirect:/user/show-contacts/0";
    }

    //update contact form handler
    @PostMapping("/update-contact/{cid}")
    public String updateForm(@PathVariable("cid") Integer cid,Model model){
        model.addAttribute("title","Update Contact");
        Contact contact=this.contactRepository.findById(cid).get();
        model.addAttribute("contact",contact);
        return "normal/update-form";
    }

    //update contact handler
    @PostMapping("/process-update")
    public String updateHandler(@ModelAttribute Contact contact, @RequestParam("profileImage") MultipartFile multipartFile, Model model, HttpSession session, Principal principal){
        try {
            Contact oldContactDetail=this.contactRepository.findById(contact.getCid()).get();
            if(!multipartFile.isEmpty()){
                //delete old photo
                File deleteFile=new ClassPathResource("static/img").getFile();
                File file1=new File(deleteFile, oldContactDetail.getImageUrl());
                file1.delete();
                //update new photo
                File saveFile=new ClassPathResource("static/img").getFile();
                Path path=Paths.get(saveFile.getAbsolutePath()+File.separator+multipartFile.getOriginalFilename());
                Files.copy(multipartFile.getInputStream(),path, StandardCopyOption.REPLACE_EXISTING);
                contact.setImageUrl(multipartFile.getOriginalFilename());
            }
            else{
                contact.setImageUrl(oldContactDetail.getImageUrl());
            }
            User currUser=this.userRepository.getUserByUserName(principal.getName());
            contact.setUser(currUser);
            this.contactRepository.save(contact);
            session.setAttribute("message", new Message("Your contact is updated !!", "success"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("");
        return "redirect:/user/"+contact.getCid()+"/contact";
    }

    //user-profile handler
    @GetMapping("/profile")
    public String userProfile(Model model){
        model.addAttribute("title", "Profile Page");
        return "normal/profile";
    } 

    //open settings handler
    @GetMapping("/settings")
    public String openSettings(){
        return "normal/settings";
    }

    //change-password handler
    @PostMapping("/change-password")
    public String changePassword(@RequestParam("oldPassword") String oldPassword, @RequestParam("newPassword") String newPassword, Principal principal, HttpSession session){
        System.out.println("Old: "+oldPassword);
        System.out.println("New: "+newPassword);
        String userName=principal.getName();
        User user=this.userRepository.getUserByUserName(userName);
        if(this.bCryptPasswordEncoder.matches(oldPassword, user.getPassword())){
            user.setPassword(this.bCryptPasswordEncoder.encode(newPassword));
            this.userRepository.save(user);
            session.setAttribute("message", new Message("Password changed successfully !!", "success"));

        }
        else{
            session.setAttribute("message", new Message("Old password didn't match !!", "danger"));
            return "redirect:/user/settings";
        } 
        return "redirect:/user/index";
    }

}
