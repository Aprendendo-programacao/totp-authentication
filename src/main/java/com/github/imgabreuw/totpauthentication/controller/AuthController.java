package com.github.imgabreuw.totpauthentication.controller;

import com.github.imgabreuw.totpauthentication.model.User;
import com.github.imgabreuw.totpauthentication.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;

import java.util.Optional;

@Controller
@SessionAttributes("loggedUser")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/register")
    public String showRegister() {
        return "register";
    }

    @PostMapping("/register")
    public String doRegister(@RequestParam String nome, @RequestParam String email, @RequestParam String senha, Model model) {
        if (userRepository.findByEmail(email).isPresent()) {
            model.addAttribute("mensagem", "Email já cadastrado!");
            return "success";
        }

        User user = new User();
        user.setNome(nome);
        user.setEmail(email);
        user.setSenha(senha);

        userRepository.save(user);

        model.addAttribute("mensagem", "Cadastro realizado com sucesso!");
        return "success";
    }

    @GetMapping("/login")
    public String showLogin() {
        return "login";
    }

    @PostMapping("/login")
    public String doLogin(@RequestParam String email, @RequestParam String senha, Model model) {
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isPresent() && userOpt.get().getSenha().equals(senha)) {
            model.addAttribute("loggedUser", userOpt.get());

            if (userOpt.get().isTotpEnabled()) {
                return "redirect:/totp/verify";
            }

            return "redirect:/totp/setup";
        }

        model.addAttribute("mensagem", "Credenciais inválidas!");
        return "login";
    }

    @GetMapping("/logout")
    public String logout(SessionStatus status) {
        status.setComplete();
        return "redirect:/login";
    }
}

