# PATRONES GOF IMPLEMENTADOS EN EL PROYECTO SIA

---

## 🔴 PATRÓN 1: STRATEGY

**¿Qué es?** 
Diferentes formas de hacer la misma cosa. Cambias el algoritmo sin cambiar el código que lo usa.

**Dónde está:**
```
src/main/java/com/example/SIA/service/
  ├── UsuarioService.java (interfaz - contrato)
  ├── impl/
  │   └── UsuarioServiceImpl.java (implementación concreta)
```

**Código:**

**Interfaz (el contrato):**
```java
// src/main/java/com/example/SIA/service/UsuarioService.java
public interface UsuarioService {
    List<UsuarioResponse> listarUsuarios();
    UsuarioResponse crearUsuario(UsuarioRequest req);
}
```

**Implementación (una estrategia concreta):**
```java
// src/main/java/com/example/SIA/service/impl/UsuarioServiceImpl.java
@Service
public class UsuarioServiceImpl implements UsuarioService {
    
    @Override
    public List<UsuarioResponse> listarUsuarios() {
        // ESTRATEGIA 1: obtener usuarios
        return usuarioRepo.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    public UsuarioResponse crearUsuario(UsuarioRequest req) {
        // ESTRATEGIA 2: crear usuario
        Usuario u = new Usuario();
        u.setNombres(req.getNombres());
        // ...
    }
}
```

**Cómo lo usa el controlador:**
```java
// src/main/java/com/example/SIA/controller/UsuarioController.java
@Controller
public class UsuarioController {
    
    @Autowired
    private UsuarioService usuarioService;  // ← Inyecta la interfaz, no la implementación
    
    @GetMapping("/usuarios")
    public String listar(Model model) {
        List<UsuarioResponse> usuarios = usuarioService.listarUsuarios();  // ← Usa la interfaz
        model.addAttribute("usuarios", usuarios);
        return "usuarios";
    }
}
```

**Por qué es Strategy?**
- La interfaz `UsuarioService` define el contrato
- `UsuarioServiceImpl` es UNA estrategia (podrías tener `UsuarioServiceAlternative` sin cambiar el controlador)
- El controlador NO conoce la implementación, solo la interfaz

**Cómo lo explicas:**
> "Usamos interfaces para definir qué puede hacer un servicio. Cualquier implementación que siga esa interfaz puede usarse. Es como tener diferentes formas de pagar (tarjeta, efectivo, PayPal), todas hacen lo mismo pero de forma diferente."

---

## 🟢 PATRÓN 2: FACADE

**¿Qué es?** 
Esconder la complejidad detrás de una interfaz simple.

**Dónde está:**
```
src/main/java/com/example/SIA/service/
├── ReporteService.java
├── EmailService.java
└── [todos los servicios son fachadas]
```

**Código - Ejemplo con ReporteService:**

```java
// src/main/java/com/example/SIA/service/ReporteService.java
@Service
@RequiredArgsConstructor
public class ReporteService {

    private final AprendizRepository aprendizRepository;
    private final EquipoRepository equipoRepository;
    
    // FACHADA SIMPLE: el controlador solo llama esto
    public byte[] generarPdfAprendicesPorPrograma() {
        // Aquí adentro está TODA la complejidad oculta
        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, baos);
        
        // ... código complejo de generar PDF, tablas, gráficas, etc.
        
        return baos.toByteArray();  // ← Devuelve algo simple
    }
}
```

**Cómo lo usa el controlador (simple):**
```java
// src/main/java/com/example/SIA/controller/ReporteController.java
@Controller
public class ReporteController {
    
    @Autowired
    private ReporteService reporteService;
    
    @GetMapping("/pdf/aprendices")
    public ResponseEntity<byte[]> verAprendices() {
        // El controlador solo llama un método simple
        byte[] pdf = reporteService.generarPdfAprendicesPorPrograma();
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=aprendices.pdf")
                .body(pdf);
    }
}
```

**La complejidad está oculta en el servicio:**
```java
// Dentro de ReporteService (el usuario del controlador NO ve esto)
private void agregarLogo(Document document) { /* ... */ }
private void agregarTitulo(Document document, String titulo) { /* ... */ }
private Image generarGraficaBarrasAprendices(Map<String, Integer> datos) { /* ... */ }
private PdfPTable crearTablaEstilizada(int columnas) { /* ... */ }
```

**Por qué es Facade?**
- El servicio ESCONDE toda la complejidad (PDF, gráficas, tablas)
- El controlador solo ve UNA PUERTA simple: `generarPdfAprendicesPorPrograma()`
- Si quieres cambiar cómo se genera el PDF, cambias adentro del servicio, el controlador NO se entera

**Cómo lo explicas:**
> "Imagina un restaurante. El camarero es la fachada. Tú no ves la cocina, solo hablas con el camarero. Dentro de la cocina hay cientos de pasos complejos (picar, cocinar, sasonar), pero tú solo pides un plato y el camarero te lo trae listo."

---

## 🔵 PATRÓN 3: FACTORY

**¿Qué es?** 
Crear objetos de forma centralizada, sin que el código sepa cómo se crean.

**Dónde está:**
```
src/main/java/com/example/SIA/config/
├── WebSocketConfig.java
└── SecurityConfig.java (si existe)
```

**Código - Ejemplo con WebSocket:**

```java
// src/main/java/com/example/SIA/config/WebSocketConfig.java
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatHandler(), "/chat/*")  // ← Factory crea el handler
                .setAllowedOrigins("*");
    }

    @Bean  // ← FACTORY METHOD
    public WebSocketHandler chatHandler() {
        return new ChatWebSocketHandler();  // ← Crea el objeto aquí
    }
}
```

**El factory es el método `chatHandler()`:**
- Está centralizado en UN lugar
- Si quieres cambiar cómo se crea `ChatWebSocketHandler`, cambias aquí
- Spring gestiona la instancia (Singleton)

**Sin el patrón (MAL):**
```java
// Tendrías que crear en cada controlador
WebSocketHandler handler = new ChatWebSocketHandler();
registry.addHandler(handler, "/chat/*");
// Si quieres cambiar la creación, cambias en 50 lugares
```

**Con el patrón (BIEN):**
```java
// Factory centralizado
registry.addHandler(chatHandler(), "/chat/*");
// Si quieres cambiar la creación, cambias en UN lugar
```

**Por qué es Factory?**
- `chatHandler()` es el método factoría
- Centraliza la creación
- Facilita cambios futuros

**Cómo lo explicas:**
> "En lugar de que cada controlador cree el objeto, hay UN lugar centralizado que lo crea. Si necesitas cambiar cómo se crea, solo cambias en ese lugar."

---

## 🟡 PATRÓN 4: TEMPLATE METHOD

**¿Qué es?** 
Define el flujo general pero deja que los pasos específicos se implementen en métodos separados.

**Dónde está:**
```
src/main/java/com/example/SIA/service/ReporteService.java
```

**Código:**

```java
// TEMPLATE METHOD - El flujo general
public byte[] generarPdfAprendicesPorPrograma() {
    try {
        Map<String, Integer> datos = new LinkedHashMap<>();
        aprendizRepository.findAll().forEach(a -> {
            String programa = a.getProgramaFormacion();
            datos.put(programa, datos.getOrDefault(programa, 0) + 1);
        });

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, baos);

        document.open();

        // TEMPLATE: Pasos en orden
        agregarLogo(document);                              // PASO 1
        agregarTitulo(document, "REPORTE: APRENDICES");    // PASO 2

        if (!datos.isEmpty()) {
            Image grafica = generarGraficaBarrasAprendices(datos);  // PASO 3
            document.add(grafica);
        }

        PdfPTable table = crearTablaEstilizada(2);         // PASO 4
        table.addCell(celdaHeader("Programa"));
        table.addCell(celdaHeader("Total"));
        datos.forEach((k, v) -> {
            table.addCell(celdaNormal(k));
            table.addCell(celdaNormal(v.toString()));
        });
        document.add(table);

        document.close();
        return baos.toByteArray();

    } catch (Exception e) {
        throw new RuntimeException("Error generando PDF", e);
    }
}

// PASOS IMPLEMENTADOS EN MÉTODOS SEPARADOS
private void agregarLogo(Document document) { /* ... */ }
private void agregarTitulo(Document document, String titulo) { /* ... */ }
private Image generarGraficaBarrasAprendices(Map<String, Integer> datos) { /* ... */ }
private PdfPTable crearTablaEstilizada(int columnas) { /* ... */ }
```

**El flujo general (template) es:**
1. Obtener datos
2. Crear documento PDF
3. Agregar logo
4. Agregar título
5. Agregar gráfica
6. Agregar tabla
7. Cerrar documento

**Los pasos específicos están en métodos separados:**
- `agregarLogo()` - cómo se agrega el logo
- `agregarTitulo()` - cómo se agrega el título
- `generarGraficaBarrasAprendices()` - cómo se genera la gráfica

**Por qué es Template Method?**
- El método `generarPdfAprendicesPorPrograma()` es el "template"
- Define la ESTRUCTURA pero NO todos los detalles
- Los detalles están en métodos privados

**Cómo lo explicas:**
> "Es como una receta de cocina. La receta dice: '1. Prepara los ingredientes, 2. Calienta el horno, 3. Mezcla, 4. Hornea'. Cada paso es un método separado. El orden es fijo (template), pero cada paso puede cambiar sin afectar a los otros."

---

## ⚫ PATRÓN 5: DAO (Data Access Object)

**¿Qué es?** 
Abstrae el acceso a datos para que el servicio no sepa cómo funciona la BD.

**Dónde está:**
```
src/main/java/com/example/SIA/repository/
├── UsuarioRepository.java
├── AprendizRepository.java
├── EquipoRepository.java
└── [todos los repositories]
```

**Código:**

```java
// src/main/java/com/example/SIA/repository/UsuarioRepository.java
@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    
    // DAO: métodos para acceder a datos
    List<Usuario> findByEstado(Integer estado);
    List<Usuario> findByPerfil(Perfil perfil);
    Optional<Usuario> findByNombreUsuario(String nombreUsuario);
}
```

**Cómo lo usa el servicio:**
```java
// src/main/java/com/example/SIA/service/impl/UsuarioServiceImpl.java
@Service
public class UsuarioServiceImpl implements UsuarioService {
    
    private final UsuarioRepository usuarioRepo;  // ← DAO inyectado
    
    @Override
    public List<UsuarioResponse> listarUsuarios() {
        // El servicio NO conoce detalles de BD
        // Solo usa el DAO
        return usuarioRepo.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
}
```

**Sin el patrón (MAL - el servicio sabe de BD):**
```java
public List<UsuarioResponse> listarUsuarios() {
    // Connection conn = DriverManager.getConnection(...);
    // Statement stmt = conn.createStatement();
    // ResultSet rs = stmt.executeQuery("SELECT * FROM usuario");
    // [Código complejo de JDBC]
}
```

**Con el patrón (BIEN - el servicio delega):**
```java
public List<UsuarioResponse> listarUsuarios() {
    return usuarioRepo.findAll();  // ← DAO hace todo
}
```

**Por qué es DAO?**
- Separa la lógica de negocio de la lógica de BD
- Si cambias de BD (MySQL a PostgreSQL), cambias en UN lugar (el DAO)
- El servicio NO se entera

**Cómo lo explicas:**
> "El DAO es un intermediario entre el servicio y la base de datos. El servicio dice 'dame todos los usuarios activos' y el DAO se encarga de hacer la query, parsear resultados, etc. Si cambias la BD, solo cambias el DAO."

---

## 🟣 PATRÓN 6: SINGLETON

**¿Qué es?** 
Una clase que tiene UNA SOLA INSTANCIA en toda la aplicación.

**Dónde está:**
```
TODA la app usa Singleton gracias a Spring:
- @Service → Singleton
- @Repository → Singleton
- @Component → Singleton
```

**Código:**

```java
// src/main/java/com/example/SIA/service/EmailService.java
@Service  // ← Spring crea UNA instancia de esta clase
public class EmailService {
    
    @Autowired
    private JavaMailSender mailSender;  // ← Compartido por TODOS
    
    public boolean enviarCorreoMasivo(List<String> destinatarios, 
                                      String asunto, 
                                      String mensaje, 
                                      String remitente) {
        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(remitente);
            mail.setTo(destinatarios.toArray(new String[0]));
            mail.setSubject(asunto);
            mail.setText(mensaje);
            
            mailSender.send(mail);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
```

**Cómo se usa:**

```java
// Controlador 1
@Controller
public class EmailController {
    @Autowired
    private EmailService emailService;  // ← MISMA INSTANCIA
}

// Controlador 2
@Controller
public class OtroController {
    @Autowired
    private EmailService emailService;  // ← MISMA INSTANCIA (compartida)
}
```

**Sin Singleton (MAL):**
```java
EmailService service1 = new EmailService();
EmailService service2 = new EmailService();
EmailService service3 = new EmailService();
// 3 instancias diferentes = más memoria, inconsistencias
```

**Con Singleton (BIEN):**
```java
@Autowired
private EmailService emailService;  // ← Toda la app comparte 1 instancia
```

**Por qué es Singleton?**
- Ahorra memoria (1 instancia en lugar de N)
- Consistencia (todos usan el mismo objeto)
- Gestión centralizada por Spring

**Cómo lo explicas:**
> "Spring crea una única instancia de cada servicio y la comparte entre toda la aplicación. Es como tener UN servidor de correos que todos usan, en lugar de que cada controlador tuviera su propio servidor."

---

## 🟠 PATRÓN 7: ADAPTER

**¿Qué es?** 
Adaptar una interfaz existente a otra que espera el código.

**Dónde está:**
```
src/main/java/com/example/SIA/config/
├── WebSocketConfig.java
├── WebConfig.java
└── SecurityConfig.java (si existe)
```

**Código - Ejemplo WebSocketConfig:**

```java
// src/main/java/com/example/SIA/config/WebSocketConfig.java
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {  // ← ADAPTER
    
    // Spring espera un WebSocketConfigurer
    // Implementamos sus métodos
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatHandler(), "/chat/*")
                .setAllowedOrigins("*");
    }

    @Bean
    public WebSocketHandler chatHandler() {
        return new ChatWebSocketHandler();
    }
}
```

**Otro ejemplo - WebConfig (para interceptores):**

```java
// src/main/java/com/example/SIA/config/WebConfig.java
@Configuration
public class WebConfig implements WebMvcConfigurer {  // ← ADAPTER
    
    @Autowired
    private NoCacheInterceptor noCacheInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Spring espera que implementemos addInterceptors
        registry.addInterceptor(noCacheInterceptor)
                .addPathPatterns("/dashboard/**");
    }
}
```

**Sin Adapter (MAL):**
```java
// No podrías registrar WebSockets fácilmente
// Tendrías que hacer cosas complicadas
```

**Con Adapter (BIEN):**
```java
// Solo implementas la interfaz que Spring espera
public class WebSocketConfig implements WebSocketConfigurer {
    @Override
    public void registerWebSocketHandlers(...) {
        // Spring sabe qué hacer aquí
    }
}
```

**Por qué es Adapter?**
- Adaptamos nuestra configuración a lo que Spring espera
- Hacemos que `ChatWebSocketHandler` sea compatible con Spring

**Cómo lo explicas:**
> "Imagina que tienes un teléfono con enchufe tipo C pero la pared tiene tipo A. El adaptador (el config) permite que funcione. Adaptamos nuestro código a lo que Spring espera."

---

## 🔶 PATRÓN 8: DECORATOR

**¿Qué es?** 
Envolver un objeto para agregar comportamiento sin modificar su estructura.

**Dónde está:**
```
src/main/java/com/example/SIA/config/NoCacheInterceptor.java
```

**Código:**

```java
// src/main/java/com/example/SIA/config/NoCacheInterceptor.java
@Component
public class NoCacheInterceptor implements HandlerInterceptor {
    
    @Override
    public boolean preHandle(HttpServletRequest request, 
                            HttpServletResponse response, 
                            Object handler) throws Exception {
        // DECORADOR: Envuelve la respuesta agregando headers
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");
        return true;  // Continúa con la petición
    }
}
```

**Cómo se registra:**
```java
// src/main/java/com/example/SIA/config/WebConfig.java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Autowired
    private NoCacheInterceptor noCacheInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // El interceptor DECORA todas las peticiones a /dashboard/**
        registry.addInterceptor(noCacheInterceptor)
                .addPathPatterns("/dashboard/**");
    }
}
```

**Sin el patrón (MAL - modificas cada controlador):**
```java
@GetMapping("/dashboard")
public String dashboard() {
    response.setHeader("Cache-Control", "...");
    response.setHeader("Pragma", "...");
    // Repites esto en 50 métodos
    return "dashboard";
}
```

**Con el patrón (BIEN - el decorator lo hace):**
```java
@GetMapping("/dashboard")
public String dashboard() {
    // El interceptor automáticamente agrega los headers
    return "dashboard";
}
```

**Por qué es Decorator?**
- Envuelve las peticiones HTTP
- Agrega comportamiento (headers de no-cache)
- Transparente para los controladores

**Cómo lo explicas:**
> "Es como envoltura de regalo. El regalo (la petición HTTP) sigue siendo el mismo, pero agregamos una capa (headers) que lo envuelve. Cualquier petición que pase por el interceptor automáticamente recibe esa decoración."

---

## 🟦 PATRÓN 9: OBSERVER (Reactive Pattern)

**¿Qué es?** 
Uno o más objetos reaccionan cuando otro objeto cambia de estado.

**Dónde está:**
```
src/main/java/com/example/SIA/websocket/ChatWebSocketHandler.java
```

**Código:**

```java
// src/main/java/com/example/SIA/websocket/ChatWebSocketHandler.java
public class ChatWebSocketHandler extends TextWebSocketHandler {
    
    // OBSERVER: Reacciona cuando hay un evento (mensaje del cliente)
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) 
            throws IOException {
        
        String payload = message.getPayload();
        
        // El handler OBSERVA cambios en el socket
        // Cuando llega un mensaje, REACCIONA
        
        // Procesa el mensaje
        // Envía respuesta
        
        for (WebSocketSession sess : sessions) {
            if (sess.isOpen()) {
                sess.sendMessage(new TextMessage("Respuesta: " + payload));
            }
        }
    }
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // OBSERVER: Reacciona cuando se conecta un cliente
        sessions.add(session);
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) 
            throws Exception {
        // OBSERVER: Reacciona cuando se desconecta un cliente
        sessions.remove(session);
    }
}
```

**Sin el patrón (MAL - polling):**
```java
// Tienes que preguntar constantemente "¿hay mensaje?"
while (true) {
    if (hasMessage()) {
        handleMessage();
    }
    Thread.sleep(100);  // Pregunta cada 100ms
}
```

**Con el patrón (BIEN - observer):**
```java
// El handler reacciona automáticamente cuando hay evento
@Override
protected void handleTextMessage(WebSocketSession session, TextMessage message) {
    // Se ejecuta automáticamente cuando llega un mensaje
}
```

**Por qué es Observer?**
- El `ChatWebSocketHandler` es el observer
- El WebSocket es el subject
- Cuando hay cambios (mensaje, conexión, desconexión), el observer reacciona

**Cómo lo explicas:**
> "Es como una campana en un restaurante. Cuando la comida está lista, la campana suena (evento). El camarero que está observando reacciona y va a buscar la comida. No pregunta constantemente '¿está lista?', solo espera a que suene."

---

## 🟩 PATRÓN 10: CHAIN OF RESPONSIBILITY

**¿Qué es?** 
Pasar una solicitud a través de una cadena de objetos que pueden manejarla.

**Dónde está:**
```
src/main/java/com/example/SIA/exception/GlobalExceptionHandler.java
```

**Código:**

```java
// src/main/java/com/example/SIA/exception/GlobalExceptionHandler.java
@ControllerAdvice  // ← Punto central de la cadena
public class GlobalExceptionHandler {
    
    // CADENA DE RESPONSABILIDAD: Excepciones se manejan aquí centralmente
    
    @ExceptionHandler(BadGatewayException.class)
    public String handleBadGateway(Model model, BadGatewayException ex) {
        model.addAttribute("mensaje", ex.getMessage() != null 
            ? ex.getMessage() 
            : "Error de comunicación con el servidor externo.");
        return "error/502";  // ← Maneja BadGatewayException
    }
    
    @ExceptionHandler(LoginException.class)
    public String handleLogin(Model model, LoginException ex) {
        model.addAttribute("mensaje", ex.getMessage());
        return "error/login";  // ← Maneja LoginException
    }
}
```

**Las excepciones son parte de la cadena:**
```
1. Excepción en Controller
    ↓
2. ¿Hay @ExceptionHandler para BadGatewayException?
    ↓ (SÍ)
3. GlobalExceptionHandler.handleBadGateway()
    ↓
4. Devuelve vista de error
```

**Sin el patrón (MAL - manejas en cada método):**
```java
@GetMapping("/api/data")
public String getData() {
    try {
        // lógica
    } catch (BadGatewayException e) {
        return "error/502";
    } catch (LoginException e) {
        return "error/login";
    }
    // Repites try-catch en 50 métodos
}
```

**Con el patrón (BIEN - manejo centralizado):**
```java
@GetMapping("/api/data")
public String getData() {
    // Si hay excepción, la cadena la maneja automáticamente
    // No necesitas try-catch
}
```

**Por qué es Chain of Responsibility?**
- Las excepciones pasan por una cadena
- `GlobalExceptionHandler` es un eslabón de la cadena
- Cada `@ExceptionHandler` puede manejar un tipo específico

**Cómo lo explicas:**
> "Es como una línea de atención al cliente. Llamas y te atiende la primera persona. Si no puede resolver, pasa a la siguiente. Las excepciones funcionan igual: pasan por una cadena de manejadores hasta que alguien las resuelve."

---

## 📌 TABLA RÁPIDA PARA EXPLICAR

| Patrón | Ubicación | Código clave | Para qué |
|--------|-----------|--------------|---------|
| **Strategy** | `service/` + `impl/` | `interface UsuarioService` + `UsuarioServiceImpl` | Diferentes formas de hacer algo |
| **Facade** | `service/ReporteService` | `generarPdfAprendicesPorPrograma()` | Esconder complejidad |
| **Factory** | `config/WebSocketConfig` | `@Bean public WebSocketHandler chatHandler()` | Crear objetos centralizadamente |
| **Template Method** | `ReporteService` | Método que llama a `agregarLogo()`, `agregarTitulo()`, etc. | Flujo general, pasos específicos |
| **DAO** | `repository/` | `UsuarioRepository extends JpaRepository` | Abstrae acceso a BD |
| **Singleton** | `@Service`, `@Repository` | `@Service public class EmailService` | UNA instancia en toda la app |
| **Adapter** | `config/WebSocketConfig` | `implements WebSocketConfigurer` | Adaptar nuestra lógica a Spring |
| **Decorator** | `config/NoCacheInterceptor` | `implements HandlerInterceptor` | Agregar comportamiento a peticiones |
| **Observer** | `websocket/ChatWebSocketHandler` | `extends TextWebSocketHandler` | Reaccionar a eventos del cliente |
| **Chain of Responsibility** | `exception/GlobalExceptionHandler` | `@ControllerAdvice` | Manejar excepciones centralmente |

---

## 📊 RESUMEN VISUAL

```
PROYECTO SIA - PATRONES GOF

┌─────────────────────────────────────────────────────┐
│  CREACIONALES (Cómo se crean los objetos)          │
├─────────────────────────────────────────────────────┤
│  ✓ FACTORY: @Bean public WebSocketHandler()        │
│  ✓ SINGLETON: @Service, @Repository (1 instancia)  │
└─────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────┐
│  ESTRUCTURALES (Cómo se organizan los objetos)     │
├─────────────────────────────────────────────────────┤
│  ✓ ADAPTER: implements WebMvcConfigurer            │
│  ✓ FACADE: ReporteService (interfaz simple)        │
│  ✓ DAO: Repository (abstrae BD)                    │
│  ✓ DECORATOR: NoCacheInterceptor (agrega behavior) │
└─────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────┐
│  DE COMPORTAMIENTO (Cómo se comunican)             │
├─────────────────────────────────────────────────────┤
│  ✓ STRATEGY: interface + implementaciones          │
│  ✓ TEMPLATE METHOD: método plantilla + pasos       │
│  ✓ OBSERVER: WebSocketHandler (reacciona eventos)  │
│  ✓ CHAIN OF RESPONSIBILITY: GlobalExceptionHandler │
└─────────────────────────────────────────────────────┘
```

---

**FIN DEL DOCUMENTO**

Generado: 6 de diciembre de 2025
Proyecto: SIA (Sistema de Información Académica)
