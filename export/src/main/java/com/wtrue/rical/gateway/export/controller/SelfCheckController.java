package com.wtrue.rical.gateway.export.controller;

import com.alibaba.fastjson.JSON;
import com.wtrue.rical.gateway.export.pojo.SelfCheckRequest;
import lombok.SneakyThrows;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.lang.reflect.Method;

/**
 * 本类是用作bean方法自检
 * @Author： Luzelong
 * @Created： 2023/2/1 10:44
 */
@RestController
public class SelfCheckController implements BeanFactoryAware {

    /**
     * 业务方引入该export包，就会把业务方自己的appname放进去
     */
    @Value("spring.application.name")
    private String appName;

    private BeanFactory beanFactory;

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }




    @RequestMapping("selfCheck.json")
    public String testInvoke(@RequestBody @Valid SelfCheckRequest request) {
        try {
            //1.安全检测
            invokeSafeCheck(request);

            //2.获取目标bean
            Class<?> beanClazz = Class.forName(request.getBeanClazzName());
            Object tartgetBean = getTartgetBean(beanClazz);

            //3.转化参数类型列表
            Class[] parameterTyps = transformParameterTypes(request.getParameterTypes());

            //4.执行
            Method method = beanClazz.getMethod(request.getMethodName(), parameterTyps);
            Object returnValue = method.invoke(tartgetBean, request.getParameters());

            return JSON.toJSONString(returnValue);

        }catch (Exception e){
            return "自检异常：" + e.getMessage();
        }
    }









    /**
     * 转化参数类型列表
     * @param parameterTypes
     * @return
     */
    @SneakyThrows
    private Class[] transformParameterTypes(String[] parameterTypes) {
        if (ArrayUtils.isEmpty(parameterTypes)) {
            return new Class[0];
        }
        Class[] classes = new Class[parameterTypes.length];
        int index = 0;
        for (String parameterType : parameterTypes) {
            Class<?> paramClzz = Class.forName(parameterType);
            classes[index++] = paramClzz;
        }
        return classes;
    }




    /**
     * 获取目标bean
     * @param beanClazz
     * @return
     */
    private Object getTartgetBean(Class<?> beanClazz) {
        Object bean = beanFactory.getBean(beanClazz);
        if (bean ==  null){
            throw new RuntimeException("本服务没有自检所需要的bean");
        }
        return bean;
    }




    /**
     * 执行前的安全检测
     * @param request
     */
    private void invokeSafeCheck(SelfCheckRequest request) {
        String targetAppName = request.getAppName();
        if (!targetAppName.equals(appName)) {
            throw new RuntimeException("自检错误！本服务不是目标服务，请检查appName是否传递错误～");
        }
    }


}
