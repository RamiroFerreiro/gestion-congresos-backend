package com.tfi.gestion_congresos_backend.services.impl;

import org.springframework.transaction.annotation.Transactional;

import com.tfi.gestion_congresos_backend.enums.EmailChangeStatus;
import com.tfi.gestion_congresos_backend.dtos.auth.ChangeEmailRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.congress_participant.AdminParticipantRegistrationRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.user.AdminCreateUserRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.user.ChangePasswordRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.user.CompleteFirstLoginRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.user.MessageResponseDTO;
import com.tfi.gestion_congresos_backend.dtos.user.UpdateUserRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.user.UserRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.user.UserResponseDTO;
import com.tfi.gestion_congresos_backend.entities.EmailChangeToken;
import com.tfi.gestion_congresos_backend.entities.User;
import com.tfi.gestion_congresos_backend.exception.ArgumentNotValidException;
import com.tfi.gestion_congresos_backend.exception.InvalidCredentialsException;
import com.tfi.gestion_congresos_backend.exception.ResourceAlreadyExistsException;
import com.tfi.gestion_congresos_backend.exception.ResourceNotFoundException;
import com.tfi.gestion_congresos_backend.exception.UserDisabledException;
import com.tfi.gestion_congresos_backend.repository.UserRepository;
import com.tfi.gestion_congresos_backend.security.SecurityEvaluator;
import com.tfi.gestion_congresos_backend.repository.CongressRepository;
import com.tfi.gestion_congresos_backend.repository.EmailChangeTokenRepository;
import com.tfi.gestion_congresos_backend.services.AuthorizationService;
import com.tfi.gestion_congresos_backend.services.CongressParticipantService;
import com.tfi.gestion_congresos_backend.services.EmailService;
import com.tfi.gestion_congresos_backend.services.RoleService;
import com.tfi.gestion_congresos_backend.services.UserService;
import com.tfi.gestion_congresos_backend.utils.DateUtils;
import com.tfi.gestion_congresos_backend.mapper.UserMapper;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final CongressRepository congressRepository;
    private final EmailChangeTokenRepository emailChangeTokenRepository;
    private final EmailService emailService;
    private final CongressParticipantService congressParticipantService;
    private final AuthorizationService authorizationService;
    private final RoleService roleService;
    private final SecurityEvaluator securityEvaluator;
    
    private static final int CODE_LENGTH = 6;

    ///----------------------------------------------------------GET----------------------------------------------------------///
    /// TRAER TODOS LOS USUARIOS DEL SISTEMA
    @Override
    public List<UserResponseDTO> getAllUsers(){

        // 1. Obtener la entidad del usuario autenticado
        User authenticatedUser = authorizationService.getAuthenticatedUserEntity();

        List<User> users;
        
        // 2. Si es Súper Admin obtiene los usuarios, sino lanza excepción
        if (securityEvaluator.isSuperAdmin(authenticatedUser)){

            users = userRepository.findAll();
        }else{

            throw new UserDisabledException("No tienes permisos de administración.");
        }
        
        return users.stream().map(userMapper::toUserResponseDTO).toList();
    }

    ///TRAER EL DTO DEL USUARIO AUTENTICADO
    @Override
    public UserResponseDTO getAuthenticatedUser() {

        User user = authorizationService.getAuthenticatedUserEntity();

        return userMapper.toUserResponseDTO(user);
    }

    ///----------------------------------------------------------CREATE----------------------------------------------------------///
    ///CREAR/REGISTRAR USUARIO
    @Transactional
    @Override
    public UserResponseDTO createUser(UserRequestDTO request){

        //Validar que coincidan las contraseña, lanza excepcion
        validatePasswordConfirmation(request.getPassword(), request.getConfirmPassword());

        //Verificar que no esté registrado el email
        if(userRepository.existsByEmail(request.getEmail())){
            throw new ResourceAlreadyExistsException( "Ya existe un usuario con ese email.");
        }

        //Mapear DTO a entidad
        User user = userMapper.toEntity(request);
        
        //Genearar el código del usuario:
        user.setCode(generateUserCode());

        //Activar al usuario
        user.setEnabled(true);
        user.setMustChangePassword(false);
        
        //Encriptar la contraseña y la guardamos
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user = userRepository.save(user);

        //Retornar de entidad a DTO
        UserResponseDTO result = userMapper.toUserResponseDTO(user);
        return result;
    }

    @Transactional
    @Override
    public UserResponseDTO adminCreateUser(
            String congressCode,
            AdminCreateUserRequestDTO request) {

        // 1. Obtener el usuario administrador autenticado
        User authenticatedUser = authorizationService.getAuthenticatedUserEntity();
        
        // 2. Verificar que el congreso exista
        if (!congressRepository.existsByCode(congressCode)) {
            throw new ResourceNotFoundException(
                    "No se encontró un congreso con el código: " + congressCode
            );
        }

        // 3. Validar permisos del administrador sobre el congreso
        if (!securityEvaluator.isAdminOfCongress(authenticatedUser, congressCode)) {
            throw new UserDisabledException(
                    "No posee permisos de administración sobre el congreso " + congressCode
            );
        }

        // 4. Verificar que el email no exista previamente
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResourceAlreadyExistsException(
                    "Ya existe un usuario registrado con el email: " + request.getEmail()
            );
        }

        // 5. Validar que el rol solicitado pueda asignarse
        roleService.validateAssignableRole(request.getNewRole());

        // 6. Generar contraseña temporal segura
        String temporaryPassword = generateRandomPassword();

        // 7. Mapear y configurar la entidad User
        User user = userMapper.toEntity(request);

        user.setPassword(passwordEncoder.encode(temporaryPassword));
        user.setMustChangePassword(true);
        user.setEnabled(true);
        user.setCode(generateUserCode());

        // 8. Persistir el usuario global
        User savedUser = userRepository.save(user);

        // 9. Construir la solicitud de inscripción administrativa
        AdminParticipantRegistrationRequestDTO participantRequest =
                AdminParticipantRegistrationRequestDTO.builder()
                        .userCode(savedUser.getCode())
                        .role(request.getNewRole())
                        .build();

        // 10. Delegar la creación de la participación
        congressParticipantService.registerParticipantByAdmin(
                congressCode,
                participantRequest
        );

        // 11. Enviar email con las credenciales temporales
        emailService.sendTemporaryPasswordEmail(savedUser, temporaryPassword);

        // 12. Retornar el usuario creado
        return userMapper.toUserResponseDTO(savedUser);
    }

    // Método privado para la generación de la contraseña temporal
    private String generateRandomPassword() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }

    @Override
    @Transactional
    public MessageResponseDTO completeFirstLogin(CompleteFirstLoginRequestDTO request) {

        // 1. Obtener el usuario autenticado
        User principalUser = authorizationService.getAuthenticatedUserEntity();

        // 2. Recuperar la entidad gestionada por JPA
        User user = getUserByUserId(principalUser.getUserId());

        // 3. Verificar que realmente sea su primer login
        if (!user.isMustChangePassword()) {
            throw new ArgumentNotValidException(
                    "El usuario ya ha completado su primer inicio de sesión."
            );
        }

        // 4. Construir el DTO para reutilizar la lógica de cambio de contraseña
        ChangePasswordRequestDTO changePasswordDTO =
                new ChangePasswordRequestDTO(
                        request.getCurrentPassword(),
                        request.getNewPassword(),
                        request.getConfirmPassword()
                );

        // 5. Ejecutar las validaciones y actualizar la contraseña
        changePassword(changePasswordDTO);

        // 6. Completar los datos globales del usuario
        user.setDni(request.getDni());
        user.setCountry(request.getCountry());

        userRepository.save(user);

        // 7. Retornar confirmación
        return new MessageResponseDTO(
                "Perfil y contraseña completados con éxito."
        );
    }

    ///----------------------------------------------------------DELETE----------------------------------------------------------///
    
    @Transactional
    @Override
    public void deleteUser(String userCode) {
        
        //1. Obtener la entidad del Usuario autenticado mediante el helper
        User authenticatedUser = authorizationService.getAuthenticatedUserEntity();

        //2. Validar que sea estrictamente Súper Admin
        if (!securityEvaluator.isSuperAdmin(authenticatedUser)) {
            throw new UserDisabledException("Solo un súper administrador del sistema puede realizar la baja de usuarios.");
        }

        //3. Buscar el usuario solicitado por código
        User targetUser = userRepository.findByCode(userCode).orElseThrow(() ->
                    new ResourceNotFoundException( "Usuario no encontrado con código: " + userCode));

        //4. Desactivar usuario
        targetUser.setEnabled(false);

        userRepository.save(targetUser);
    }

    ///----------------------------------------------------------UPDATE----------------------------------------------------------///
    
    @Override
    @Transactional
    public UserResponseDTO updateUser(String userCode, UpdateUserRequestDTO userRequestDTO) {

        // 1. Obtener la entidad del Usuario autenticado mediante el helper
        User authenticatedUser = authorizationService.getAuthenticatedUserEntity();

        // 2. Buscar el usuario solicitado por código
        User targetUser = userRepository.findByCode(userCode)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con código: " + userCode));

        // 3. Evaluar permisos: Solo superadmin, todos los usuarios consigo mismo
        boolean isSelf = authenticatedUser.getCode().equals(userCode);
        boolean isSuperAdmin = securityEvaluator.isSuperAdmin(authenticatedUser);

        if (!isSelf && !isSuperAdmin) {
            throw new UserDisabledException("No tienes permisos para modificar la información de este usuario.");
        }

        ///4. Actualiza la entidad con los datos del DTO
        userMapper.updateUserFromDto(userRequestDTO, targetUser);

        ///5. Guardamos en la bd y devolvemos el DTO
        User userSaved = userRepository.save(targetUser);
        return userMapper.toUserResponseDTO(userSaved);
    }
    
    @Override
    @Transactional
    public MessageResponseDTO changePassword(ChangePasswordRequestDTO request){

        // 1. Obtener la entidad desprendida del SecurityContext
        User principalUser = authorizationService.getAuthenticatedUserEntity();

        // 2. Cargar la entidad gestionada por JPA desde la base de datos
        User user = getUserByUserId(principalUser.getUserId());

        // 3. Validaciones de negocio
        validateCurrentPassword(user.getPassword(), request.getCurrentPassword());
        validatePasswordConfirmation(request.getNewPassword(), request.getConfirmPassword());
        validateNewPassword(user.getPassword(), request.getNewPassword());
        
        // 4. Actualizar contraseña y flag
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setMustChangePassword(false); 

        // 5. Persistir (opcional explicitar save si usás @Transactional)
        userRepository.save(user);
        return new MessageResponseDTO("Contraseña cambiada con éxito");
    }

    @Override
    @Transactional
    public MessageResponseDTO changeEmail(ChangeEmailRequestDTO request) {
        
        //1. Obtener la entidad desprendida del usuario autenticado desde el SecurityContext
        User authenticatedUser = authorizationService.getAuthenticatedUserEntity();

        //2. Cargar la entidad gestionada por JPA desde la BD para asegurar el estado real del usuario
        User user = getUserByUserId(authenticatedUser.getUserId());

        //3. Validar que la contraseña actual enviada en el DTO coincida con la contraseña encriptada en la BD
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("La contraseña actual no es válida.");
        }

        //4. Verificar que el nuevo email no esté utilizado
        if (userRepository.existsByEmail(request.getNewEmail())) {
            throw new ResourceAlreadyExistsException("El email ya está registrado.");
        }
        
        //5. Buscar si el usuario ya tenía una solicitud previa de cambio de email pendiente de completar
        EmailChangeToken emailChangeToken = emailChangeTokenRepository.findByUser(user)
            .orElseGet(EmailChangeToken::new); //Si no existe, instancia una nueva entidad token
        
        //6. Seteo de entidad EmailChangeToken
        emailChangeToken.setUser(user);
        emailChangeToken.setNewEmail(request.getNewEmail());
        emailChangeToken.setToken(UUID.randomUUID().toString());
        emailChangeToken.setExpirationDate(DateUtils.now().plusMinutes(30));
        emailChangeToken.setStatus(EmailChangeStatus.PENDING_CURRENT_EMAIL);

        //7. Persistir o actualizar la entidad del token en la base de datos
        emailChangeTokenRepository.save(emailChangeToken);
        //8. Invocar al servicio de mensajería para enviar el mail de confirmación a la casilla original
        emailService.sendCurrentEmailChangeVerificationEmail(user,emailChangeToken.getToken());

        //9. Retornar el mensaje explicativo al cliente indicando el inicio del proceso
        return MessageResponseDTO.builder()
            .message("Se ha enviado un enlace de confirmación a tu email actual.").build();
    }

    ///----------------------------------------------------------BOOLEAN----------------------------------------------------------///
   
    /// Determinar si existe un usuario por su CODE:
    @Override
	@Transactional(readOnly = true)
	public boolean existsByCode(String code) {
		return userRepository.existsByCode(code);
	}

    ///----------------------------------------------------------PRIVADOS----------------------------------------------------------///

    private void validateCurrentPassword(String encodedUserPassword, String currentPassword){
        if (!passwordEncoder.matches(currentPassword, encodedUserPassword)) {

            throw new ArgumentNotValidException("La contraseña actual es incorrecta");
        }
    }

     private void validatePasswordConfirmation(String newPassword, String confirmPassword){
        if (newPassword == null || !newPassword.equals(confirmPassword)) {

            throw new ArgumentNotValidException("Las contraseñas no coinciden");
        }
    }

    private void validateNewPassword(String encodedCurrentPassword, String newPassword){
        if (passwordEncoder.matches(newPassword, encodedCurrentPassword)) {

            throw new ArgumentNotValidException("La contraseña nueva debe ser diferente a la actual");
        }
    }
    
    /// Genera un código de usuario no duplicado:
  	private String generateUserCode() {
          String code;
          do {
              code = generateRandomNumericCode(CODE_LENGTH);
          } while (userRepository.existsByCode(code));
          return code;
    }

  	/// Genera un código numérico aleatorio de la longitud indicada:
	private String generateRandomNumericCode(int length) {
	    int max = (int) Math.pow(10, length) - 1;
	    int randomNumber = new Random().nextInt(max + 1);
	    return String.format("%0" + length + "d", randomNumber);
	}


    ///---------------------------------------------------- PUBLICOS AUXILIARES ---------------------------------------------------///
     @Override
    public UserResponseDTO getUserById(Long userId){

        User user = userRepository.findById(userId).orElseThrow(() ->
                    new ResourceNotFoundException( "Usuario no encontrado con ID: " + userId));
        
        UserResponseDTO result = userMapper.toUserResponseDTO(user);

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserByUserId(Long userId){

        User user = userRepository.findById(userId).orElseThrow(() ->
                    new ResourceNotFoundException("Usuario no encontrado con ID: " + userId));

        return user;
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserByUserCode(String code){
    	
    	User user = userRepository.findByCode(code).orElseThrow(() ->
    	new ResourceNotFoundException("Usuario no encontrado con código: " + code));
    	
    	return user;
    }
}