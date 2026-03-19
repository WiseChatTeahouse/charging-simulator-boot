package chat.wisechat.charging.service;

import chat.wisechat.charging.entity.EmulatorMessage;
import chat.wisechat.charging.exception.BusinessException;
import chat.wisechat.charging.mapper.EmulatorMessageMapper;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 模拟器消息服务类
 *
 * @author siberia.hu
 * @date 2026/3/19
 */
@Slf4j
@Service
public class EmulatorMessageService {

    @Autowired
    private EmulatorMessageMapper emulatorMessageMapper;

    /**
     * 新增消息
     */
    @Transactional(rollbackFor = Exception.class)
    public EmulatorMessage save(EmulatorMessage message) {
        emulatorMessageMapper.insert(message);
        log.info("新增模拟器消息: id={}, groupId={}, stepOrder={}", message.getId(), message.getGroupId(), message.getStepOrder());
        return message;
    }

    /**
     * 更新消息
     */
    @Transactional(rollbackFor = Exception.class)
    public EmulatorMessage update(EmulatorMessage message) {
        if (emulatorMessageMapper.selectById(message.getId()) == null) {
            throw new BusinessException("消息不存在: id=" + message.getId());
        }
        emulatorMessageMapper.updateById(message);
        log.info("更新模拟器消息: id={}", message.getId());
        return message;
    }

    /**
     * 删除消息
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (emulatorMessageMapper.selectById(id) == null) {
            throw new BusinessException("消息不存在: id=" + id);
        }
        emulatorMessageMapper.deleteById(id);
        log.info("删除模拟器消息: id={}", id);
    }

    /**
     * 按 ID 查询
     */
    public EmulatorMessage getById(Long id) {
        EmulatorMessage message = emulatorMessageMapper.selectById(id);
        if (message == null) {
            throw new BusinessException("消息不存在: id=" + id);
        }
        return message;
    }

    /**
     * 按 groupId 查询，按 stepOrder 升序
     */
    public List<EmulatorMessage> listByGroupId(Integer groupId) {
        return emulatorMessageMapper.selectByGroupIdOrdered(groupId);
    }

    /**
     * 查询所有 groupId
     */
    public List<Integer> listGroupIds() {
        return emulatorMessageMapper.selectDistinctGroupIds();
    }

    /**
     * 删除某个 group 下的所有消息
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteByGroupId(Integer groupId) {
        LambdaQueryWrapper<EmulatorMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EmulatorMessage::getGroupId, groupId);
        int count = emulatorMessageMapper.delete(wrapper);
        log.info("删除 groupId={} 的消息 {} 条", groupId, count);
    }

    @Transactional
    public void uploadMessage(MultipartFile file) {
        String json = "";
        try {
            json = new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("文件读取失败!");
            throw new RuntimeException(e);
        }

        JSONObject jsonObject = JSONObject.parseObject(json);

        JSONObject data = jsonObject.getJSONObject("data");
        JSONObject value = data.getJSONObject("value");
        JSONArray values = value.getJSONArray("values");
        // 摘出需要的报文数据
        long groupId = System.currentTimeMillis();
        for (int i = 0; i < values.size(); i++) {
            EmulatorMessage emulatorMessage = new EmulatorMessage();
            emulatorMessage.setPayload(JSON.toJSONString(values.get(i)));
            emulatorMessage.setGroupId(groupId);
            emulatorMessage.setStepOrder(i + 1);
            save(emulatorMessage);
        }
    }

}
