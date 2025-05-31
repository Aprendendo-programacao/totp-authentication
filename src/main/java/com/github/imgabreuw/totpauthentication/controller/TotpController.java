package com.github.imgabreuw.totpauthentication.controller;

import com.github.imgabreuw.totpauthentication.model.User;
import com.github.imgabreuw.totpauthentication.repository.UserRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.binary.Base32;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.util.Base64;

@Controller
@SessionAttributes("loggedUser")
@RequiredArgsConstructor
public class TotpController {

    private final UserRepository userRepository;

    // Gera um segredo TOTP aleatório (base32)
    private String generateSecret() {
        byte[] buffer = new byte[20];
        new SecureRandom().nextBytes(buffer);
        return new Base32().encodeToString(buffer);
    }

    @GetMapping("/totp/setup")
    public String showTotpSetup(@ModelAttribute("loggedUser") User user, Model model) {
        if (user.getTotpSecret() == null || user.getTotpSecret().isEmpty()) {
            String secret = generateSecret();
            user.setTotpSecret(secret);
            userRepository.save(user);
        }

        // QR Code: otpauth://totp/{issuer}:{email}?secret={secret}&issuer={issuer}
        String issuer = "TOTPApp";
        String otpauth = String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s", issuer, user.getEmail(), user.getTotpSecret(), issuer);

        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(otpauth, BarcodeFormat.QR_CODE, 200, 200);
            BufferedImage qrImage = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < 200; x++) {
                for (int y = 0; y < 200; y++) {
                    qrImage.setRGB(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                }
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(qrImage, "png", baos);
            String base64Qr = Base64.getEncoder().encodeToString(baos.toByteArray());
            model.addAttribute("qrCodeDataUrl", "data:image/png;base64," + base64Qr);
        } catch (Exception e) {
            model.addAttribute("qrCodeDataUrl", null);
        }

        model.addAttribute("totpSecret", user.getTotpSecret());
        return "totp-setup";
    }

    @PostMapping("/totp/enable")
    public String enableTotp(@ModelAttribute("loggedUser") User user, @RequestParam String totpCode, Model model) {
        String secret = user.getTotpSecret();

        if (isValidTotpCode(secret, totpCode)) {
            user.setTotpEnabled(true);
            userRepository.save(user);
            model.addAttribute("mensagem", "2FA ativado com sucesso!");
            return "success";
        } else {
            model.addAttribute("mensagem", "Código TOTP inválido!");
            return "totp-setup";
        }
    }

    private boolean isValidTotpCode(String secret, String code) {
        try {
            // Decodifica a secret em base32
            Base32 base32 = new Base32();
            byte[] secretBytes = base32.decode(secret);

            long timeIndex = System.currentTimeMillis() / 1000 / 30;
            for (int i = -1; i <= 1; i++) { // tolerância de 1 intervalo para relógios fora de sincronia
                String generatedCode = generateTotpCode(secretBytes, timeIndex + i);
                if (generatedCode.equals(code)) {
                    return true;
                }
            }
        } catch (Exception ignored) {
        }

        return false;
    }

    private String generateTotpCode(byte[] key, long timeIndex) throws Exception {
        byte[] data = new byte[8];
        for (int i = 7; i >= 0; i--) {
            data[i] = (byte) (timeIndex & 0xFF);
            timeIndex >>= 8;
        }

        Mac mac = Mac.getInstance("HmacSHA1");
        SecretKeySpec signKey = new SecretKeySpec(key, "HmacSHA1");
        mac.init(signKey);
        byte[] hash = mac.doFinal(data);

        int offset = hash[hash.length - 1] & 0xF;
        int binary =
                ((hash[offset] & 0x7F) << 24) |
                ((hash[offset + 1] & 0xFF) << 16) |
                ((hash[offset + 2] & 0xFF) << 8) |
                (hash[offset + 3] & 0xFF);

        int otp = binary % 1000000;
        return String.format("%06d", otp);
    }

    @GetMapping("/totp/verify")
    public String showTotpVerify() {
        return "totp-verify";
    }

    @PostMapping("/totp/verify")
    public String verifyTotp(@ModelAttribute("loggedUser") User user, @RequestParam String totpCode, Model model) {
        String secret = user.getTotpSecret();

        if (isValidTotpCode(secret, totpCode)) {
            model.addAttribute("mensagem", "Login realizado com sucesso!");
            return "success";
        } else {
            model.addAttribute("mensagem", "Código TOTP inválido!");
            return "totp-verify";
        }
    }
}
