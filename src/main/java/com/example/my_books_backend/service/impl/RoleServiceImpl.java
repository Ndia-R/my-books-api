package com.example.my_books_backend.service.impl;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.my_books_backend.dto.role.RoleRequest;
import com.example.my_books_backend.dto.role.RoleResponse;
import com.example.my_books_backend.entity.Role;
import com.example.my_books_backend.entity.enums.RoleName;
import com.example.my_books_backend.exception.NotFoundException;
import com.example.my_books_backend.mapper.RoleMapper;
import com.example.my_books_backend.repository.RoleRepository;
import com.example.my_books_backend.service.RoleService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoleServiceImpl implements RoleService {
    private final RoleRepository roleRepository;
    private final RoleMapper roleMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public List<RoleResponse> getAllRoles() {
        List<Role> roles = roleRepository.findAll();
        return roleMapper.toRoleResponseList(roles);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RoleResponse getRoleById(Long id) {
        Role role = roleRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Role not found"));
        return roleMapper.toRoleResponse(role);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public RoleResponse createRole(RoleRequest request) {
        Role role = new Role();
        role.setName(request.getName());
        role.setDescription(request.getDescription());
        Role savedRole = roleRepository.save(role);
        return roleMapper.toRoleResponse(savedRole);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public RoleResponse updateRole(Long id, RoleRequest request) {
        Role role = roleRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Role not found"));

        RoleName name = request.getName();
        String description = request.getDescription();

        if (name != null) {
            role.setName(name);
        }

        if (description != null) {
            role.setDescription(description);
        }
        Role savedRole = roleRepository.save(role);
        return roleMapper.toRoleResponse(savedRole);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void deleteRole(Long id) {
        if (!roleRepository.existsById(id)) {
            throw new NotFoundException("Role not found");
        }
        roleRepository.deleteById(id);
    }
}
