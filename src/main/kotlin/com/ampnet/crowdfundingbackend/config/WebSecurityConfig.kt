package com.ampnet.crowdfundingbackend.config

import com.ampnet.crowdfundingbackend.config.auth.CustomAuthenticationProvider
import com.ampnet.crowdfundingbackend.config.auth.JwtAuthenticationEntryPoint
import com.ampnet.crowdfundingbackend.config.auth.JwtAuthenticationFilter
import com.ampnet.crowdfundingbackend.config.auth.ProfileFilter
import com.ampnet.crowdfundingbackend.enums.PrivilegeType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
class WebSecurityConfig(
    val unauthorizedHandler: JwtAuthenticationEntryPoint,
    val authenticationTokenFilter: JwtAuthenticationFilter,
    val profileFilter: ProfileFilter
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

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowedOrigins = listOf("*")
        configuration.allowedMethods = listOf(
                HttpMethod.HEAD.name,
                HttpMethod.GET.name,
                HttpMethod.POST.name,
                HttpMethod.PUT.name,
                HttpMethod.DELETE.name
        )
        configuration.allowedHeaders = listOf(
                HttpHeaders.AUTHORIZATION,
                HttpHeaders.CONTENT_TYPE,
                HttpHeaders.CACHE_CONTROL
        )

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }

    override fun configure(http: HttpSecurity) {
        http.cors().and().csrf().disable()
                .authorizeRequests()
                .antMatchers("/actuator/health").permitAll()
                .antMatchers("/docs/index.html").permitAll()
                .antMatchers("/actuator/**").hasAnyAuthority(PrivilegeType.MONITORING.name)
                .antMatchers("/token/**", "/signup").permitAll()
                .antMatchers("/countries/**").permitAll()
                .antMatchers("/mail-confirmation").permitAll()
                .antMatchers("/mail-check").permitAll()
                .anyRequest().authenticated()
                .and()
                .exceptionHandling().authenticationEntryPoint(unauthorizedHandler).and()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        http
                .addFilterBefore(authenticationTokenFilter, UsernamePasswordAuthenticationFilter::class.java)
                .addFilterAfter(profileFilter, JwtAuthenticationFilter::class.java)
    }
}
