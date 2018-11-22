package com.ampnet.crowdfundingbackend.config

import mu.KLogging
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.AsyncConfigurer
import org.springframework.scheduling.annotation.EnableAsync
import java.util.concurrent.Executor
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.lang.reflect.Method

@Configuration
@EnableAsync
class SpringAsyncConfig : AsyncConfigurer {

    override fun getAsyncExecutor(): Executor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 1
        executor.maxPoolSize = 5
        executor.setQueueCapacity(100)
        executor.setThreadNamePrefix("AsyncExecutor-")
        executor.initialize()
        return executor
    }

    override fun getAsyncUncaughtExceptionHandler(): AsyncUncaughtExceptionHandler? {
        return CustomAsyncExceptionHandler()
    }
}

class CustomAsyncExceptionHandler : AsyncUncaughtExceptionHandler {

    companion object : KLogging()

    override fun handleUncaughtException(ex: Throwable, method: Method, vararg params: Any?) {
        logger.warn(ex) { "Uncaught exception method name: ${method.name}" }
        params.iterator().forEach {
            logger.warn { "Parameter value = $it" }
        }
    }
}
