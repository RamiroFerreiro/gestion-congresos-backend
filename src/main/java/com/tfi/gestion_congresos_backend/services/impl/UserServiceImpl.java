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
import com.tfi.gestion_congresos_backend.entities.EmailChangeToken;
import com.tfi.gestion_congresos_backend.entities.Role;
import com.tfi.gestion_congresos_backend.entities.User;
import com.tfi.gestion_congresos_backend.exception.ArgumentNotValidException;
import com.tfi.gestion_congresos_backend.exception.InvalidCredentialsException;
import com.tfi.gestion_congresos_backend.exception.ResourceAlreadyExistsException;
import com.tfi.gestion_congresos_backend.exception.ResourceNotFoundException;
import com.tfi.gestion_congresos_backend.repository.UserRepository;
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
import java.util.UUID;

import org.apache.coyote.BadRequestException;
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
    private final RoleService roleService;

    ///----------------------------------------------------------GET----------------------------------------------------------///
    //@PreAuthorize("hasRole('ADMINISTRATOR')")
    @Override
    public List<UserResponseDTO> getAllUsers(){
        
        List<User> users = userRepository.findAll();

        //stream para transformar List<User> en List<UserResponse> 
        List<UserResponseDTO> result = users.stream()
                                    .map(userMapper::toUserResponseDTO)
                                    .toList();

        return result;
    }

    //@PreAuthorize("hasRole('ADMINISTRATOR')")
    @Override
    public UserResponseDTO getUserById(Long userId){

        User user = userRepository.findById(userId).orElseThrow(() ->
                    new ResourceNotFoundException( "Usuario no encontrado con ID: " + userId));
        
        UserResponseDTO result = userMapper.toUserResponseDTO(user);

        return result;
    }

    //@PreAuthorize("hasRole('ADMINISTRATOR')")
    @Override
    @Transactional(readOnly = true)
    public User getUserByUserId(Long userId){

        User user = userRepository.findById(userId).orElseThrow(() ->
                    new ResourceNotFoundException("Usuario no encontrado con ID: " + userId));

        return user;
    }

    @Override
	@Transactional(readOnly = true)
	/// Obtener participantes de un congreso con determinado rol:
	public List<UserResponseDTO> getParticipantsByCongressAndRole(Long congressId, RoleName role) {
		
    	// Validar existencia del congreso:
        if (!congressRepository.existsById(congressId)) {
            throw new ResourceNotFoundException("Congreso no encontrado con el ID: " + congressId);
        }
    	
    	List<User> participants = userRepository.findParticipantsByCongressIdAndRole(congressId, role);
		
		List<UserResponseDTO> result = participants.stream()
				.map(userMapper::toUserResponseDTO)
				.toList();
		
		return result;
	}

    @Override
    public UserResponseDTO getAuthenticatedUser() {

        User user = getAuthenticatedUserEntity(); // método privado

        return userMapper.toUserResponseDTO(user);
    }

    ///----------------------------------------------------------CREATE----------------------------------------------------------///

    //@PreAuthorize("hasRole('ADMINISTRATOR')")
    @Transactional
    @Override
    public UserResponseDTO createUser(UserRequestDTO request){

        //Validar que coincidan las contraseña, lanza excepcion
        validatePasswordConfirmation(request.getPassword(), request.getConfirmPassword());

        //verificamos que no esté registrado el mail
        if(userRepository.existsByEmail(request.getEmail())){
            throw new ResourceAlreadyExistsException( "Ya existe un usuario con ese email.");
        }

        //Buscamos el rol, si no existe lanza excepción
        Role role = roleRepository.findById(request.getRoleId())
            .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado"));
        
        //Traemos la lista de roles permitidos (DTOs)
        List<RoleResponseDTO> allowedRoles = roleService.getRegisterableRoles();

        //Verificamos si algún DTO de la lista coincide con el ID seleccionado
        boolean isRoleAllowed = allowedRoles.stream() .anyMatch(allowedRole -> allowedRole.getRoleId().equals(role.getRoleId()));

        //Lanzamos excepcion en caso de que el rol no sea permitido
        if (!isRoleAllowed) {
            throw new ArgumentNotValidException("No tienes permisos para registrarte con el rol seleccionado.");
        }

        //mapeamos DTO a entidad
        User user = userMapper.toEntity(request);
        
        //lo seteamos
        user.setRole(role);

        //activamos al usuario
        user.setEnabled(true);
        
        //Encriptamos la contraseña y la guardamos
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user = userRepository.save(user);

        //retorno de entidad a DTO
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

        System.out.println("HOLA1");

        // Mapear DTO a Entidad mediante MapStruct
        User user = userMapper.toEntity(request);

        System.out.println("HOLA2");

        // Asignar manualmente los campos procesados (rol y contraseña encriptada)
        user.setRole(role);
        user.setPassword(passwordEncoder.encode(temporaryPassword));

         System.out.println("HOLA3");

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
    
    //@PreAuthorize("hasRole('ADMINISTRATOR')")
    @Override
    public void deleteUser(Long userId) {
    
        User user = userRepository.findById(userId).orElseThrow(() ->
                    new ResourceNotFoundException( "Usuario no encontrado con ID: " + userId));

        user.setEnabled(false);

        userRepository.save(user);
    }

    ///----------------------------------------------------------UPDATE----------------------------------------------------------///
    
    //@PreAuthorize("hasRole('ADMINISTRATOR')")
    @Override
    public UserResponseDTO updateUser(Long userId, UpdateUserRequestDTO userRequestDTO) {

        ///Se busca el usuario, si no existe lanza excepción
        User user = userRepository.findById(userId).orElseThrow(() -> 
                    new ResourceNotFoundException("Usuario no encontrado"));

        ///Actualiza la entidad con los datos del DTO
        userMapper.updateUserFromDto(userRequestDTO, user);

        ///Guardamos en la bd y devolvemos el DTO
        user = userRepository.save(user);
        UserResponseDTO result = userMapper.toUserResponseDTO(user);

        return result;
    }

    //@PreAuthorize("hasRole('ADMINISTRATOR')")
    @Transactional
    @Override
    public UserResponseDTO updateUserRole(Long userId, Long roleId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + userId));

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado con ID: " + roleId));

        user.setRole(role);
        User updatedUser = userRepository.save(user);

        return userMapper.toUserResponseDTO(updatedUser);
    }
    
    @Override
    public MessageResponseDTO changePassword(ChangePasswordRequestDTO request){

        User user = getAuthenticatedUserEntity();
        
        validateCurrentPassword(user.getPassword(), request.getCurrentPassword());
        validatePasswordConfirmation(request.getNewPassword(), request.getConfirmPassword());
        validateNewPassword(user.getPassword(), request.getNewPassword());
        
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));

        userRepository.save(user);

        return new MessageResponseDTO("Contraseña cambiada con éxito");
    }

    @Override
    public MessageResponseDTO changeEmail(ChangeEmailRequestDTO request) {
        
        User user = getAuthenticatedUserEntity();

        // Validar contraseña actual
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("La contraseña actual no es válida.");
        }

        // Verificar que el nuevo email no esté utilizado
        if (userRepository.existsByEmail(request.getNewEmail())) {
            throw new ResourceAlreadyExistsException("El email ya está registrado.");
        }
        
        Optional<EmailChangeToken> existingToken = emailChangeTokenRepository.findByUser(user);

        EmailChangeToken emailChangeToken;

        if (existingToken.isPresent()) {
            emailChangeToken = existingToken.get();
        } else {
            emailChangeToken = new EmailChangeToken();
        }
        
        emailChangeToken.setUser(user);
        emailChangeToken.setNewEmail(request.getNewEmail());
        emailChangeToken.setToken(UUID.randomUUID().toString());
        emailChangeToken.setExpirationDate(DateUtils.now().plusMinutes(30));
        emailChangeToken.setStatus(EmailChangeStatus.PENDING_CURRENT_EMAIL);


        emailChangeTokenRepository.save(emailChangeToken);


        emailService.sendCurrentEmailChangeVerificationEmail(user,emailChangeToken.getToken());

        return MessageResponseDTO.builder()
            .message("Se ha enviado un enlace de confirmación a tu email actual.").build();
    }

    ///----------------------------------------------------------BOOLEAN----------------------------------------------------------///
   
    /// Determinar si existe un usuario por su ID:
    @Override
	@Transactional(readOnly = true)
	public boolean existsById(Long userId) {
		return userRepository.existsById(userId);
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

    
}