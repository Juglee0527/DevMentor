package com.devmentor.user.service;

import com.devmentor.common.exception.ResourceNotFoundException;
import com.devmentor.skill.entity.Skill;
import com.devmentor.skill.repository.SkillRepository;
import com.devmentor.user.dto.UserRequest;
import com.devmentor.user.dto.UserResponse;
import com.devmentor.user.entity.User;
import com.devmentor.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final SkillRepository skillRepository;

    public UserService(UserRepository userRepository, SkillRepository skillRepository) {
        this.userRepository = userRepository;
        this.skillRepository = skillRepository;
    }

    @Transactional
    public UserResponse create(UserRequest request) {
        User user = new User(
                request.nickname().trim(),
                request.careerYears(),
                request.currentRole().trim(),
                request.learningGoal().trim()
        );
        resolveSkills(request.interestedSkillCodes()).forEach(user::addInterestedSkill);
        return UserResponse.from(userRepository.save(user));
    }

    public UserResponse get(Long userId) {
        return UserResponse.from(findUser(userId));
    }

    @Transactional
    public UserResponse update(Long userId, UserRequest request) {
        User user = findUser(userId);
        user.update(
                request.nickname().trim(),
                request.careerYears(),
                request.currentRole().trim(),
                request.learningGoal().trim(),
                new LinkedHashSet<>(resolveSkills(request.interestedSkillCodes()))
        );
        return UserResponse.from(user);
    }

    public User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));
    }

    private List<Skill> resolveSkills(Set<String> codes) {
        if (codes.isEmpty()) {
            return List.of();
        }
        List<Skill> skills = skillRepository.findAllByCodeIn(codes);
        if (skills.size() != codes.size()) {
            throw new ResourceNotFoundException("존재하지 않는 관심 기술이 포함되어 있습니다.");
        }
        return skills;
    }
}
