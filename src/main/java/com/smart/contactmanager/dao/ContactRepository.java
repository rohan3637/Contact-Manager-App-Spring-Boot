package com.smart.contactmanager.dao;

import java.util.List;

import javax.websocket.server.PathParam;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.smart.contactmanager.entities.Contact;
import com.smart.contactmanager.entities.User;

public interface ContactRepository extends JpaRepository<Contact, Integer> {
    
    @Query("from Contact as c WHERE c.user.id= :userId")
    public Page<Contact> getContactsByUser(@PathParam("userId") int userId, Pageable pagiable);

    //search
    List<Contact> findByNameContainingAndUser(String keywords, User user);
}
