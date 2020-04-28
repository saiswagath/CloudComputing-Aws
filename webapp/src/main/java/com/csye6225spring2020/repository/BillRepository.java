package com.csye6225spring2020.repository;

import com.csye6225spring2020.entity.Bill;
import com.csye6225spring2020.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import javax.transaction.Transactional;
import java.util.List;
import java.util.UUID;
@Transactional
public interface BillRepository extends JpaRepository<Bill, Long> {


    List<Bill> findByOwnerid(UUID ownerid);
    Bill findById(UUID id);
    Long deleteById(UUID billId);
}
