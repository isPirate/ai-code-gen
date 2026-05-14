package cn.pirate.aicodegen.service.impl;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import cn.pirate.aicodegen.model.entity.App;
import cn.pirate.aicodegen.mapper.AppMapper;
import cn.pirate.aicodegen.service.AppService;
import org.springframework.stereotype.Service;

/**
 * 应用 服务层实现。
 *
 * @author <a href="https://github.com/isPirate">isPirate</a>
 */
@Service
public class AppServiceImpl extends ServiceImpl<AppMapper, App>  implements AppService{

}
