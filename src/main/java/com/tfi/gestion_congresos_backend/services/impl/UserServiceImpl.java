package com.tfi.gestion_congresos_backend.services.impl;

import org.springframework.transaction.annotation.Transactional;

import com.tfi.gestion_congresos_backend.enums.EmailChangeStatus;
import com.tfi.gestion_congresos_backend.enums.RoleName;
import com.tfi.gestion_congresos_backend.dtos.RoleResponseDTO;
import com.tfi.gestion_congresos_backend.dtos.auth.ChangeEmailRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.user.AdminCreateUserRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.user.ChangePasswordRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.user.MessageResponseDTO;
import com.tfi.gestion_congresos_backend.dtos.user.UpdateUserRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.user.UserRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.user.UserResponseDTO;
import com.tfi.gestion_congresos_backend.entities.CongressParticipant;
import com.tfi.gestion_congresos_backend.entities.EmailChangeToken;
import com.tfi.gestion_congresos_backend.entities.Role;
import com.tfi.gestion_congresos_backend.entities.User;
import com.tfi.gestion_congresos_backend.exception.ArgumentNotValidException;
import com.tfi.gestion_congresos_backend.exception.InvalidCredentialsException;
import com.tfi.gestion_congresos_backend.exception.ResourceAlreadyExistsException;
import com.tfi.gestion_congresos_backend.exception.ResourceNotFoundException;
import com.tfi.gestion_congresos_backend.exception.UserDisabledException;
import com.tfi.gestion_congresos_backend.repository.UserRepository;
import com.tfi.gestion_congresos_backend.security.SecurityEvaluator;
import com.tfi.gestion_congresos_backend.repository.CongressParticipantRepository;
import com.tfi.gestion_congresos_backend.repository.CongressRepository;
import com.tfi.gestion_congresos_backend.repository.EmailChangeTokenRepository;
import com.tfi.gestion_congresos_backend.repository.PasswordResetTokenRepository;
import com.tfi.gestion_congresos_backend.repository.RoleRepository;
import com.tfi.gestion_congresos_backend.services.EmailService;
import com.tfi.gestion_congresos_backend.services.RoleService;
import com.tfi.gestion_congresos_backend.services.UserService;
import com.tfi.gestion_congresos_backend.utils.DateUtils;
import com.tfi.gestion_congresos_backend.mapper.UserMapper;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import org.apache.coyote.BadRequestException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final CongressRepository congressRepository;
    private final EmailChangeTokenRepository emailChangeTokenRepository;
    private final EmailService emailService;


    private final SecurityEvaluator securityEvaluator;

    private final CongressParticipantRepository participantRepository;
    
    private static final int CODE_LENGTH = 6;

    ///----------------------------------------------------------GET----------------------------------------------------------///
    /// TRAER TODOS LOS USUARIOS DEL SISTEMA
    @Override
    public List<UserResponseDTO> getAllUsers(){

        // 1. Obtener la entidad del usuario autenticado
        User authenticatedUser = getAuthenticatedUserEntity();

        List<User> users;
        
        // 2. Si es Súper Admin obtiene los usuarios, sino lanza excepción
        if (securityEvaluator.isSuperAdmin(authenticatedUser)){

            users = userRepository.findAll();
        }else{

            throw new UserDisabledException("No tienes permisos de administración.");
        }
        
        return users.stream().map(userMapper::toUserResponseDTO).toList();
    }

    ///TRAER TODOS LOS USUARIOS DE UN DETERMINADO CONGRESO
    @Override
    public List<UserResponseDTO> getUsersByCongress(String congressCode) {

        // 1. Obtener el usuario autenticado
        User authenticatedUser = getAuthenticatedUserEntity();

        // 2. Validar autorización (Fail-Fast)
        if (!securityEvaluator.isAdminOfCongress(authenticatedUser, congressCode)) {
            throw new UserDisabledException("No tienes permisos de administración sobre el congreso especificado.");
        }

        // 3. Buscar usuarios y mapear a DTOs
        return participantRepository.findUsersByCongressCode(congressCode)
                .stream()
                .map(userMapper::toUserResponseDTO)
                .toList();
        }

    ///TRAER EL USUARIO DE UN DETERMINADO CONGRESO
    @Override
    public UserResponseDTO getUserByCodeAndCongress(String congressCode, String userCode){
        
        // 1. Obtener la entidad del usuario autenticado
        User authenticatedUser = getAuthenticatedUserEntity();

        // 2. Buscar al usuario objetivo por su código global
        User targetUser = userRepository.findByCode(userCode)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con código: " + userCode));

        // 3. Validar que el usuario objetivo pertenezca al congreso (Aplica para TODOS)
        boolean belongsToCongress = securityEvaluator.isParticipantOfCongress(targetUser, congressCode);
        if (!belongsToCongress) {
            throw new ResourceNotFoundException("El usuario no pertenece al congreso especificado.");
        }
        
        // 4. Validar permisos de acceso (Self OR SuperAdmin OR AdminOfCongress)
        boolean isSelf = authenticatedUser.getCode().equals(userCode);
        boolean canAccess = isSelf || securityEvaluator.isAdminOfCongress(authenticatedUser, congressCode);

        if (!canAccess) {
            throw new UserDisabledException("No tienes permisos para acceder a la información de este usuario.");
        }

        // 5. Mapear y retornar DTO
        return userMapper.toUserResponseDTO(targetUser);
    }

    ///TRAER EL USUARIO AUTENTICADO
    @Override
    public UserResponseDTO getAuthenticatedUser() {

        User user = getAuthenticatedUserEntity(); // método privado

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
    public UserResponseDTO adminCreateUser(AdminCreateUserRequestDTO request) {


        // Validar que el email no exista previamente
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResourceAlreadyExistsException("Ya existe un usuario registrado con el email: " + request.getEmail());
        }

        // Buscar el rol, si no existe lanza excepción
        Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado con ID: " + request.getRoleId()));

        // Generar contraseña temporal segura de 10 caracteres
        String temporaryPassword = generateRandomPassword();

        // Mapear DTO a Entidad mediante MapStruct
        User user = userMapper.toEntity(request);

        // Asignar manualmente los campos procesados (rol y contraseña encriptada)
        user.setPassword(passwordEncoder.encode(temporaryPassword));
        user.setMustChangePassword(true);

        // Persistir en la base de datos
        User savedUser = userRepository.save(user);

        // Enviar e-mail con la contraseña temporal
        emailService.sendTemporaryPasswordEmail(savedUser, temporaryPassword);

        return userMapper.toUserResponseDTO(savedUser);
    }

    // Método privado para la generación de la contraseña temporal
    private String generateRandomPassword() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }

    ///----------------------------------------------------------DELETE----------------------------------------------------------///
    
    @Transactional
    @Override
    public void deleteUser(String userCode) {
        
        //1. Obtener la entidad del Usuario autenticado mediante el helper
        User authenticatedUser = getAuthenticatedUserEntity();

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
        User authenticatedUser = getAuthenticatedUserEntity();

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

    @Transactional
    @Override
    public void updateUserRole(String congressCode, String userCode, RoleName newRole) {

        //1. Obtener la entidad del usuario autenticado
        User authenticatedUser = getAuthenticatedUserEntity();

        //2. Validar si es Admin o Super Admin
        if(!securityEvaluator.isAdminOfCongress(authenticatedUser, congressCode)) {
            throw new UserDisabledException("No tienes permisos de administración sobre este congreso para modificar roles.");
        }

        //3. Buscar la entidad Role correspondiente al Enum enviado
        Role roleEntity = roleRepository.findByName(newRole)
                .orElseThrow(() -> new ResourceNotFoundException("El rol especificado no existe en el sistema."));


        //4. Traer y validar si participante es miembro activo del congreso
        CongressParticipant targetParticipant = participantRepository
            .findByUser_CodeAndCongress_CodeAndActiveTrue(userCode, congressCode)
            .orElseThrow(() -> new ResourceNotFoundException(
            "No se encontró una inscripción activa para el usuario " + userCode + " en el congreso " + congressCode));

        //5. Actualizar la relación en la tabla intermedia y guardar
        targetParticipant.setRole(roleEntity);
        participantRepository.save(targetParticipant);
    }
    
    @Override
    public MessageResponseDTO changePassword(ChangePasswordRequestDTO request){

        // 1. Obtener la entidad desprendida del SecurityContext
        User principalUser = getAuthenticatedUserEntity();

        // 2. Cargar la entidad gestionada por JPA desde la base de datos
        User user = getUserByUserId(principalUser.getUserId());

        // 3. Validaciones de negocio
        validateCurrentPassword(user.getPassword(), request.getCurrentPassword());
        validatePasswordConfirmation(request.getNewPassword(), request.getConfirmPassword());
        validateNewPassword(user.getPassword(), request.getNewPassword());
        
        // 4. Actualizar contraseña y flag
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setMustChangePassword(false); 

        userRepository.save(user);

        // 5. Persistir (opcional explicitar save si usás @Transactional)

        return new MessageResponseDTO("Contraseña cambiada con éxito");
    }

    @Override
    @Transactional
    public MessageResponseDTO changeEmail(ChangeEmailRequestDTO request) {
        
        //1. Obtener la entidad desprendida del usuario autenticado desde el SecurityContext
        User authenticatedUser = getAuthenticatedUserEntity();

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

    
    
    public User getAuthenticatedUserEntity() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof User)) {

            throw new InvalidCredentialsException("Usuario no autenticado");
        }

        return (User) authentication.getPrincipal();
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

    @Override
	@Transactional(readOnly = true)
	/// Obtener participantes de un congreso con determinado rol:
	public List<UserResponseDTO> getParticipantsByCongressAndRole(String code, RoleName role) {
		
    	// Validar existencia del congreso:
        if (!congressRepository.existsByCode(code)) {
            throw new ResourceNotFoundException("Congreso no encontrado con el código: " + code);
        }
    	
    	List<User> participants = userRepository.findParticipantsByCongressCodeAndRole(code, role);
		
		List<UserResponseDTO> result = participants.stream()
				.map(userMapper::toUserResponseDTO)
				.toList();
		
		return result;
	}
    
}