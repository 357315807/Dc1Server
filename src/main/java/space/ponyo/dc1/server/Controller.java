package space.ponyo.dc1.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import space.ponyo.dc1.server.bean.MyHttpResponse;
import space.ponyo.dc1.server.model.DataPool;
import space.ponyo.dc1.server.model.PlanPool;
import space.ponyo.dc1.server.model.db.PlanBean;
import space.ponyo.dc1.server.model.db.PlanDao;
import space.ponyo.dc1.server.server.ConnectionManager;


@RestController
public class Controller {
    public static final Logger logger = LoggerFactory.getLogger(Controller.class);
    /**
     * API查询设备列表
     * @param token
     * @return jsonStr
     */
    @RequestMapping(value = "/api/queryDeviceList", method = {RequestMethod.GET, RequestMethod.POST}, produces = {"text/html;charset=utf-8"})
    public String queryDeviceList(@RequestParam(name = "token") String token) {
        if (checkToken(token)) {
            return MyHttpResponse.error("token验证失败！");
        }
        return MyHttpResponse.success(DataPool.dc1Map.values());
    }

    /**
     * 查询计划列表
     * @param token
     * @param deviceId
     * @return jsonStr
     */
    @RequestMapping(value = "/api/queryPlanList", method = {RequestMethod.GET, RequestMethod.POST}, produces = {"text/html;charset=utf-8"})
    public String queryPlanList(@RequestParam(name = "token") String token, @RequestParam(name = "deviceId") String deviceId) {

        if (checkToken(token)) {
            return MyHttpResponse.error("token验证失败！");
        }
        return MyHttpResponse.success(PlanDao.getInstance().queryAllByDeviceId(deviceId));
    }

    /**
     * 添加计划
     * @param token
     * @param planBean
     * @return jsonStr
     */
    @RequestMapping(value = "/api/addPlan", method = {RequestMethod.GET, RequestMethod.POST}, produces = {"text/html;charset=utf-8"})
    public String addPlan(@RequestParam(name = "token") String token, @RequestBody PlanBean planBean) {
        if (checkToken(token)) {
            return MyHttpResponse.error("token验证失败！");
        }
        boolean b = PlanPool.getInstance().addPlan(planBean);
        if (!b) {
            return MyHttpResponse.error("添加失败");
        }
        return MyHttpResponse.success("添加成功");
    }


    private boolean checkToken(String token) {
        if (token == null || "".equals(token) || !ConnectionManager.getInstance().token.equals(token)) {
            logger.error("tip token验证失败！");
            return true;
        }
        return false;
    }
}
