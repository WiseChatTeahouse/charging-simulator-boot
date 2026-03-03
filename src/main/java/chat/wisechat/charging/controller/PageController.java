package chat.wisechat.charging.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 页面路由控制器
 *
 * @author siberia.hu
 * @date 2026/3/3
 */
@Controller
public class PageController {
    
    /**
     * 首页 - 重定向到站点列表
     */
    @GetMapping("/")
    public String index() {
        return "redirect:/stations";
    }
    
    /**
     * 站点列表页面
     */
    @GetMapping("/stations")
    public String stationList() {
        return "stations";
    }
    
    /**
     * 站点详情页面
     */
    @GetMapping("/station/{stationId}")
    public String stationDetail(@PathVariable Long stationId, Model model) {
        model.addAttribute("stationId", stationId);
        return "station-detail";
    }
    
    /**
     * 充电模拟器页面
     */
    @GetMapping("/charging/{gunId}")
    public String chargingSimulator(@PathVariable Long gunId, Model model) {
        model.addAttribute("gunId", gunId);
        return "charging-simulator";
    }
}
