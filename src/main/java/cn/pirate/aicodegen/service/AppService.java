package cn.pirate.aicodegen.service;

import cn.pirate.aicodegen.model.dto.app.AppQueryRequest;
import cn.pirate.aicodegen.model.vo.AppVO;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import cn.pirate.aicodegen.model.entity.App;

import java.util.List;

/**
 * 应用 服务层。
 *
 * @author <a href="https://github.com/isPirate">isPirate</a>
 */
public interface AppService extends IService<App> {

    QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest);

    List<AppVO> getAppVOList(List<App> appList);

    AppVO getAppVO(App app);
}
