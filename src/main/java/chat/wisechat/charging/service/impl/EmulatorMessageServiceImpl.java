package chat.wisechat.charging.service.impl;

import chat.wisechat.charging.entity.EmulatorMessage;
import chat.wisechat.charging.mapper.EmulatorMessageMapper;
import chat.wisechat.charging.service.EmulatorMessageService;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author siberia.hu
 * @date 2026/3/20
 */
@Service
public class EmulatorMessageServiceImpl extends ServiceImpl<EmulatorMessageMapper, EmulatorMessage> implements EmulatorMessageService {

    private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyyMMdd HH:mm:ss");


    @Override
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
            EmulatorMessage emulatorMessage = null;
            try {
                emulatorMessage = new EmulatorMessage();
                JSONArray payload = JSONArray.from(values.get(i));
                String type = JSONObject.from(payload.get(0)).getString("v");
                String time = JSONObject.from(payload.get(1)).getString("v");
                String payloadInfo = JSONObject.from(payload.get(3)).getString("v");
                emulatorMessage.setType(type);
                emulatorMessage.setTime(SIMPLE_DATE_FORMAT.parse(time));
                emulatorMessage.setPayload(payloadInfo);
                emulatorMessage.setGroupId(groupId);
                emulatorMessage.setStepOrder(i + 1);
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
            save(emulatorMessage);
        }
    }

    @Override
    public List<Long> getAllGroupIds() {
        QueryWrapper<EmulatorMessage> wrapper = new QueryWrapper<>();
        wrapper.select("group_id")
                .groupBy("group_id");

        return this.list(wrapper)
                .stream()
                .map(EmulatorMessage::getGroupId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public List<EmulatorMessage> getByGroupId(Long groupId) {
        LambdaQueryWrapper<EmulatorMessage> lqw = new LambdaQueryWrapper<>();
        lqw.eq(EmulatorMessage::getGroupId, groupId);
        return list(lqw);
    }
}