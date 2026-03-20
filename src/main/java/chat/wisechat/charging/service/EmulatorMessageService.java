package chat.wisechat.charging.service;

import chat.wisechat.charging.entity.EmulatorMessage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 模拟器消息服务类
 *
 * @author siberia.hu
 * @date 2026/3/19
 */

public interface EmulatorMessageService extends IService<EmulatorMessage> {

    void uploadMessage(MultipartFile file);

    List<Long> getAllGroupIds();

    List<EmulatorMessage> getByGroupId(Long randomGroupId);

}
