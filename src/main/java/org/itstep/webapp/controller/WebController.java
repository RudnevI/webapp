package org.itstep.webapp.controller;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.itstep.webapp.config.StaticConfig;
import org.itstep.webapp.entity.Book;
import org.itstep.webapp.entity.DbUser;
import org.itstep.webapp.entity.Item;
import org.itstep.webapp.entity.Role;
import org.itstep.webapp.service.BookService;
import org.itstep.webapp.service.ItemService;
import org.itstep.webapp.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

@Controller
@Slf4j
public class WebController {

  @Value("${file.upload.ava}")
  private String avaBaseUrl;

  @Value("${file.upload.defclasspath}")
  private String defaultAvaClasspath;

  @Value("${file.upload.avaclasspath}")
  private String avaClasspath;

  private final ItemService service;

  private final UserService userService;

  private final BookService bookService;



  public WebController(final ItemService service, final UserService userService, final BookService bookService) {
    this.service = service;
    this.userService = userService;
    this.bookService = bookService;
  }

  @GetMapping(value = "/") public String index(Model model) {
    List<Item> allItems = service.getAllItems();
    model.addAttribute("items", allItems);

    model.addAttribute("currentUser", getUser());

    return "index";
  }

  @GetMapping(value = "/login") public String login(Model model) {

    model.addAttribute("currentUser", getUser());
    return "login";
  }


  @PreAuthorize("hasRole('ROLE_MODERSTOR')") @GetMapping(value = "/items") public String items(Model model) {
    model.addAttribute("currentUser", getUser());
    return "items";
  }


  @PreAuthorize("hasRole('ROLE_ADMIN')") @GetMapping(value = "/admin") public String adminPage(Model model) {
    model.addAttribute("currentUser", getUser());
    return "admin";
  }


  @GetMapping(value = "accessdenied") public String accessdenied(Model model) {
    model.addAttribute("currentUser", getUser());
    return "403";
  }

  @PreAuthorize("hasRole('ROLE_ADMIN')") @PostMapping(value = "/adduser")
  public String addUser(Model model, @RequestParam(name = "email") String email, @RequestParam(name = "password") String password, @RequestParam(name = "confirm_password") String confirmPass,
      @RequestParam(name = "full_name") String fullName) {
    model.addAttribute("currentUser", getUser());

    if (password.equals(confirmPass)) {
      List<Role> roles = new ArrayList<>();
      roles.add(StaticConfig.ROLE_USER);

      DbUser user = new DbUser(null, email, password, fullName, roles, null, null);
      if (userService.registerUser(user) != null) {
        return "redirect:/users?success";
      }
    }
    return "redirect:/users?error";
  }

  @PreAuthorize("hasAnyRole('ROLE_USER')") @GetMapping(value = "viewprofilephoto/{avaHash}",
                                                       produces = {MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE})

  public @ResponseBody byte[] viewProfilePhoto(
      @PathVariable(name = "avaHash") String avaHash) throws IOException {

    String image = defaultAvaClasspath + "img.png";

    if (!avaHash.equals("null")) {
      image = avaClasspath + avaHash;
    }


    InputStream in;

    try {
      ClassPathResource classPathResource = new ClassPathResource(image);
      in = classPathResource.getInputStream();
    } catch (IOException e) {

      image = defaultAvaClasspath + "img.png";
      ClassPathResource classPathResource = new ClassPathResource(image);
      in = classPathResource.getInputStream();
      log.error(e.getMessage());
      e.printStackTrace();
    }
    return IOUtils.toByteArray(in);
  }


  @GetMapping(value = "users") public String users(Model model, @RequestParam(name = "page",
                                                                              defaultValue = "1") int page) {
    model.addAttribute("currentUser", getUser());

    int finalPage = page - 1;

    if (finalPage < 0) {
      finalPage = 0;
    }

    int usersCount = userService.getUsersCount(StaticConfig.ROLE_USER);

    int pageCount = (usersCount - 1) / StaticConfig.PAGE_SIZE + 1;

    if (pageCount < 1) {
      pageCount = 1;
    }

    List<DbUser> allUsers = userService.getAllUsersPaged(finalPage, StaticConfig.PAGE_SIZE, StaticConfig.ROLE_USER);
    model.addAttribute("users", allUsers);
    model.addAttribute("pageCount", pageCount);
    return "users";
  }

  @GetMapping(value = "profile") public String profile(Model model) {
    model.addAttribute("currentUser", getUser());

    return "profile";
  }

  @PreAuthorize("hasAnyRole('ROLE_USER')") @PostMapping(value = "uploadava") public String uploadAvatar(Model model, @RequestParam(name = "file_ava") MultipartFile file) {

    DbUser currentUser = getUser();

    if (currentUser != null) {


      if (file.getContentType().equals("image/png") || file.getContentType().equals("image/jpeg")) {
        String uniqueName = DigestUtils.sha1Hex("user_ava_" + currentUser.getId());

        String ext = "png";

        if (file.getContentType().equals("image/jpeg")) {
          ext = "jpg";
        }

        try {

          String fullPath = avaBaseUrl + uniqueName + "." + ext;
          byte[] bytes = file.getBytes();
          Path path = Paths.get(fullPath);
          Files.write(path, bytes);
          currentUser.setAva(fullPath);
          currentUser.setAvaHash(uniqueName + "." + ext);
          userService.updateUser(currentUser);

        } catch (Exception e) {
          log.error(e.getMessage());
        }
      }

    }

    return "redirect:/profile";
  }

  @GetMapping(value = "search")
  public String search(Model model,
      @RequestParam (name = "page", defaultValue = "1") int page,
      @RequestParam (name = "name", defaultValue = "", required = false) String name,
      @RequestParam (name = "author", defaultValue = "", required = false) String author,
      @RequestParam (name = "page_from", defaultValue = "0", required = false) Integer pageFrom,
      @RequestParam (name = "page_to", defaultValue = "0", required = false) Integer pageTo,
      @RequestParam (name = "price_from", defaultValue = "0", required = false) Integer priceFrom,
      @RequestParam (name = "price_to", defaultValue = "0", required = false) Integer priceTo){


    int finalPage = page - 1;

    if (finalPage < 0) {
      finalPage = 0;
    }

    Long countBooks = bookService.countBooks(name, author, pageFrom, pageTo, priceFrom, priceTo);

    Long pageCount = (countBooks - 1) / StaticConfig.PAGE_SIZE + 1;

    if (pageCount < 1) {
      pageCount = 1L;
    }

    model.addAttribute("pageCount", pageCount);

    List<Book> books = bookService.searchBook(finalPage, StaticConfig.PAGE_SIZE, name, author, pageFrom, pageTo, priceFrom, priceTo);


    model.addAttribute("books", books);

    return "search";
  }


  private DbUser getUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (!(authentication instanceof AnonymousAuthenticationToken)) {
      User user = (User) authentication.getPrincipal();
      DbUser dbUser = userService.findByEmail(user.getUsername());
      return dbUser;
    }
    return null;
  }

}
