package chat.wisechat.charging.controller;

import chat.wisechat.charging.service.EmulatorMessageService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author siberia.hu
 * @date 2026/3/19
 */
@Slf4j
@RestController
@RequestMapping("/api/emulator-message")
public class EmulatorMessageController {

    @Resource
    private EmulatorMessageService emulatorMessageService;

    @PostMapping("/uploadMessage")
    public void uploadMessage(@RequestParam("file") MultipartFile file) {
        emulatorMessageService.uploadMessage(file);
    }

}