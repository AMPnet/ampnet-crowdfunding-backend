package com.ampnet.crowdfundingbackend.config

import com.ampnet.crowdfundingbackend.config.auth.CustomAuthenticationProvider
import com.ampnet.crowdfundingbackend.config.auth.JwtAuthenticationEntryPoint
import com.ampnet.crowdfundingbackend.config.auth.JwtAuthenticationFilter
import com.ampnet.crowdfundingbackend.enums.PrivilegeType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
class WebSecurityConfig(
    val unauthorizedHandler: JwtAuthenticationEntryPoint,
    val authenticationTokenFilter: JwtAuthenticationFilter
) : WebSecurityConfigurerAdapter() {

    @Override
    @Bean
    override fun authenticationManagerBean(): AuthenticationManager {
        return super.authenticationManagerBean()
    }

    @Autowired
    fun globalUserDetails(
        authBuilder: AuthenticationManagerBuilder,
        authenticationProvider: CustomAuthenticationProvider
    ) {
        authBuilder.authenticationProvider(authenticationProvider)
    }

    override fun configure(http: HttpSecurity) {
        http.cors().and().csrf().disable()
                .authorizeRequests()
                .antMatchers("/actuator/health").permitAll()
                .antMatchers("/docs/index.html").permitAll()
                .antMatchers("/actuator/**").hasAnyAuthority(PrivilegeType.MONITORING.name)
                .antMatchers("/token/**", "/signup").permitAll()
                .antMatchers("/countries/**").permitAll()
                .anyRequest().authenticated()
                .and()
                .exceptionHandling().authenticationEntryPoint(unauthorizedHandler).and()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        http
                .addFilterBefore(authenticationTokenFilter, UsernamePasswordAuthenticationFilter::class.java)
    }
}