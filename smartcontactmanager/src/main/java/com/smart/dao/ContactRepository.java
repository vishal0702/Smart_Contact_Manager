package com.smart.dao;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.smart.entities.Contact;
import com.smart.entities.User;

import jakarta.transaction.Transactional;

public interface ContactRepository extends JpaRepository<Contact, Integer> {

	//Pagination
	//Pageable object will have two variables-
	//currentPage- page
	//contacts per page- 5
	@Query("from Contact as c where c.user.id =:userId")
	public Page<Contact> findContactsByUser(@Param("userId") int userId, Pageable pageable);
	
	
	//Delete contact
	@Modifying
	@Transactional
	@Query(value="delete from Contact c where c.cId =?1")
	public void deleteContactById(Integer cId);
	
	
	//Search Contact
	public List<Contact> findByNameContainingAndUser(String name, User user);
}
