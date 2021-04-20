package org.itstep.webapp.service.impl;

import java.util.List;

import javax.transaction.Transactional;

import org.itstep.webapp.entity.Item;
import org.itstep.webapp.repository.ItemRepo;
import org.itstep.webapp.service.ItemService;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class ItemServiceImpl implements ItemService {

  private final ItemRepo repo;

  public ItemServiceImpl(final ItemRepo repo) {
    this.repo = repo;
  }

  @Override public List<Item> getAllItems() {
    return repo.findAll();
  }
}
