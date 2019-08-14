package com.ampnet.crowdfundingbackend.config

import com.ampnet.crowdfundingbackend.config.auth.JwtAuthenticationProvider
import com.ampnet.crowdfundingbackend.config.auth.JwtAuthenticationEntryPoint
import com.ampnet.crowdfundingbackend.config.auth.JwtAuthenticationFilter
import com.ampnet.crowdfundingbackend.config.auth.ProfileFilter
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
    val profileFilter: ProfileFilter,
    val jwtAuthenticationProvider: JwtAuthenticationProvider
) : WebSecurityConfigurerAdapter() {

    @Override
    @Bean
    override fun authenticationManagerBean(): AuthenticationManager {
        return super.authenticationManagerBean()
    }

    override fun configure(auth: AuthenticationManagerBuilder) {
        auth.authenticationProvider(jwtAuthenticationProvider)
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        // TODO: change allowed origins
        configuration.allowedOrigins = listOf("*")
        configuration.allowedMethods = listOf(
            HttpMethod.HEAD.name,
            HttpMethod.GET.name,
            HttpMethod.POST.name,
            HttpMethod.PUT.name,
            HttpMethod.OPTIONS.name,
            HttpMethod.DELETE.name
        )
        configuration.allowedHeaders = listOf(
            HttpHeaders.AUTHORIZATION,
            HttpHeaders.CONTENT_TYPE,
            HttpHeaders.CACHE_CONTROL,
            HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS
        )

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }

    override fun configure(http: HttpSecurity) {
        http.cors().and().csrf().disable()
            .authorizeRequests()
            // TODO: websocket maybe should not be open
            .antMatchers("/websocket/**").permitAll()
            .antMatchers("/actuator/**").permitAll()
            .antMatchers("/docs/index.html").permitAll()
            .antMatchers("/issuer/**").permitAll()
            .antMatchers(HttpMethod.GET, "/public/**").permitAll()
            .antMatchers(HttpMethod.POST, "/wallet/pair").permitAll()
            .antMatchers(HttpMethod.GET, "/wallet/pair/*").permitAll()
            .antMatchers(HttpMethod.POST, "/tx_broadcast").permitAll()
            .anyRequest().authenticated()
            .and()
            .exceptionHandling().authenticationEntryPoint(unauthorizedHandler).and()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        http
            .addFilterBefore(authenticationTokenFilter, UsernamePasswordAuthenticationFilter::class.java)
            .addFilterAfter(profileFilter, JwtAuthenticationFilter::class.java)
    }
}
