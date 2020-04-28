package com.csye6225spring2020.repository;


import com.csye6225spring2020.entity.File;
import org.springframework.data.jpa.repository.JpaRepository;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Transactional
public interface FileRepository extends JpaRepository<File,Long> {
    File findById(UUID uuid);
    Long deleteById(UUID uuid);
    ArrayList<File> findAllByFileName(String fileName);
    File findByBillId(UUID uuid);
    Long deleteByBillId(UUID id);

}
