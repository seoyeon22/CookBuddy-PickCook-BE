package org.example.be17pickcook.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.example.be17pickcook.config.filter.LoginFilter;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * PickCook 프로젝트 Swagger/OpenAPI 설정
 * - JWT 인증 스키마 설정
 * - 커스텀 로그인 엔드포인트 등록
 * - API 기본 정보 및 서버 설정
 */
@Configuration
public class SwaggerConfig {

    /**
     * 메인 OpenAPI 설정
     */
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(serverList())
                .components(securityComponents())
                .security(securityRequirements());
    }

    /**
     * API 기본 정보 설정
     */
    private Info apiInfo() {
        return new Info()
                .title("PickCook API 명세서")
                .description("""
                        **PickCook**은 냉장고 식재료를 관리하고, 보유한 재료를 기반으로 레시피를 추천하며, 
                        부족한 재료는 바로 구매할 수 있는 통합 서비스입니다.
                        
                        ## 주요 기능
                        - **냉장고 관리**: 식재료 등록, 수정, 삭제 및 유통기한 관리
                        - **레시피 추천**: 보유 재료 기반 맞춤 레시피 제안
                        - **온라인 쇼핑**: 부족한 재료 원클릭 구매
                        - **커뮤니티**: 레시피 공유 및 요리 팁 교환
                        - **리뷰 시스템**: 상품 및 레시피 평가
                        
                        ## 인증 방식
                        - **JWT**: HttpOnly 쿠키 기반 (PICKCOOK_AT), 120분 만료 - 일반 로그인 유지
                        - **OAuth2**: 카카오 소셜 로그인 지원
                        - **이메일 인증**: UUID 기반, 24시간 만료 - 회원가입 시
                        - **비밀번호 재설정**: 이메일 발송용(30분) / 마이페이지용(10분) 토큰 -잃어버린 비밀번호 복구
                        
                        ## API 응답 형식
                        모든 API는 BaseResponse<T> 형식으로 통일된 응답을 제공합니다.
                        """)
                .version("1.0.0")
                .contact(new Contact()
                        .name("PickCook 개발팀")
                        .email("support@pickcook.kro.kr")
                        .url("https://pick-cook.kro.kr:8443"));
    }

    /**
     * 서버 환경 설정
     */
    private List<Server> serverList() {
        return Arrays.asList(
                new Server()
                        .url("https://pick-cook.kro.kr:8443")
                        .description("운영 서버"),
                new Server()
                        .url("http://localhost:8080")
                        .description("로컬 개발 서버")
        );
    }

    /**
     * JWT 보안 스키마 설정
     */
    private Components securityComponents() {
        return new Components()
                .addSecuritySchemes("bearerAuth",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT 토큰을 입력하세요 (Bearer 접두사 제외)")
                )
                .addSchemas("BaseResponse", baseResponseSchema())
                .addSchemas("ErrorResponse", errorResponseSchema());
    }

    /**
     * 전역 보안 요구사항 설정
     */
    private List<SecurityRequirement> securityRequirements() {
        return List.of(
                new SecurityRequirement().addList("bearerAuth")
        );
    }

    /**
     * BaseResponse 공통 스키마 정의
     */
    private Schema<?> baseResponseSchema() {
        return new ObjectSchema()
                .description("PickCook 표준 API 응답 형식")
                .addProperty("success",
                        new BooleanSchema()
                                .description("요청 성공 여부")
                                .example(true))
                .addProperty("code",
                        new IntegerSchema()
                                .description("응답 상태 코드")
                                .example(20000))
                .addProperty("message",
                        new StringSchema()
                                .description("응답 메시지")
                                .example("요청에 성공하였습니다."))
                .addProperty("results",
                        new ObjectSchema()
                                .description("실제 응답 데이터")
                                .nullable(true));
    }

    /**
     * ErrorResponse 스키마 정의
     */
    private Schema<?> errorResponseSchema() {
        return new ObjectSchema()
                .description("PickCook 표준 에러 응답 형식")
                .addProperty("success",
                        new BooleanSchema()
                                .description("요청 성공 여부")
                                .example(false))
                .addProperty("code",
                        new IntegerSchema()
                                .description("에러 상태 코드")
                                .example(30000))
                .addProperty("message",
                        new StringSchema()
                                .description("에러 메시지")
                                .example("입력값을 확인해주세요."))
                .addProperty("results",
                        new Schema<>()
                                .description("에러 상세 정보 (있는 경우)")
                                .nullable(true));
    }

    /**
     * 커스텀 로그인 엔드포인트 등록
     */
    @Bean
    public OpenApiCustomizer springSecurityLoginEndpointCustomizer(ApplicationContext applicationContext) {
        FilterChainProxy springSecurityFilterChain = applicationContext.getBean("springSecurityFilterChain", FilterChainProxy.class);

        return (openApi) -> {
            for (SecurityFilterChain filterChain : springSecurityFilterChain.getFilterChains()) {
                Optional<LoginFilter> filter = filterChain.getFilters().stream()
                        .filter(LoginFilter.class::isInstance)
                        .map(LoginFilter.class::cast)
                        .findAny();

                if (filter.isPresent()) {
                    Operation operation = createLoginOperation();
                    PathItem pathItem = new PathItem().post(operation);
                    openApi.getPaths().addPathItem("/login", pathItem);
                }
            }
        };
    }

    /**
     * 로그인 Operation 생성
     */
    private Operation createLoginOperation() {
        Operation operation = new Operation();

        // 요청 Body 스키마
        Schema<?> loginSchema = new ObjectSchema()
                .description("로그인 요청 정보")
                .addProperty("email",
                        new StringSchema()
                                .description("사용자 이메일")
                                .example("user@example.com"))
                .addProperty("password",
                        new StringSchema()
                                .description("사용자 비밀번호")
                                .example("password123!"));

        RequestBody requestBody = new RequestBody()
                .description("로그인 요청 Body")
                .content(new Content()
                        .addMediaType("application/json",
                                new MediaType().schema(loginSchema)));

        operation.setRequestBody(requestBody);

        // 응답 설정
        ApiResponses responses = new ApiResponses();
        responses.addApiResponse(
                String.valueOf(HttpStatus.OK.value()),
                new ApiResponse()
                        .description("로그인 성공")
                        .content(new Content()
                                .addMediaType("application/json",
                                        new MediaType().schema(new Schema<>().$ref("#/components/schemas/BaseResponse"))))
        );
        responses.addApiResponse(
                String.valueOf(HttpStatus.BAD_REQUEST.value()),
                new ApiResponse()
                        .description("로그인 실패")
                        .content(new Content()
                                .addMediaType("application/json",
                                        new MediaType().schema(new Schema<>().$ref("#/components/schemas/ErrorResponse"))))
        );
        operation.setResponses(responses);

        // 태그 및 요약 설정
        operation.addTagsItem("회원 관리");
        operation.setSummary("사용자 로그인");
        operation.setDescription("이메일과 비밀번호로 로그인을 진행합니다. 성공 시 JWT 토큰이 HttpOnly 쿠키로 설정됩니다.");

        return operation;
    }
}