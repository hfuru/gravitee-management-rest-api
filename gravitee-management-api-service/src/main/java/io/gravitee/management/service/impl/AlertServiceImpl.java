/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.management.service.impl;

import io.gravitee.management.model.alert.AlertEntity;
import io.gravitee.management.model.alert.AlertType;
import io.gravitee.management.service.AlertService;
import io.gravitee.management.service.ApiService;
import io.gravitee.management.service.alert.AlertTriggerService;
import io.gravitee.repository.management.api.AlertRepository;
import io.gravitee.repository.management.model.Alert.AlertReferenceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

import static io.gravitee.management.model.alert.AlertType.HEALTH_CHECK;

/**
 * @author Azize ELAMRANI (azize at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class AlertServiceImpl extends TransactionalService implements AlertService {

    private final Logger LOGGER = LoggerFactory.getLogger(AlertServiceImpl.class);

    @Autowired
    private AlertRepository alertRepository;
    @Autowired
    private ApiService apiService;
    @Autowired
    private AlertTriggerService alertTriggerService;

    @Override
    public List<AlertEntity> list(final AlertReferenceType referenceType, final String referenceId) {
        alertRepository.
        return null;
    }

    @Override
    public AlertEntity create(final AlertEntity alert) {
        triggerAlert(alert);
        return null;
    }

    @Override
    public AlertEntity update(final AlertEntity alert) {
        triggerAlert(alert);
        return null;
    }

    private void triggerAlert(final AlertEntity alert) {
        if (alert.isEnabled()) {
            if (HEALTH_CHECK.equals(alert.getType())) {
                alertTriggerService.triggerAPIHC(apiService.findById(alert.getReferenceId()));
            }
        } else {
            if (HEALTH_CHECK.equals(alert.getType())) {
                alertTriggerService.cancelTriggerAPIHC(apiService.findById(alert.getReferenceId()));
            }
        }
    }


//    @Override
//    public List<TagEntity> findAll() {
//        try {
//            LOGGER.debug("Find all APIs");
//            return tagRepository.findAll()
//                    .stream()
//                    .map(this::convert).collect(Collectors.toList());
//        } catch (TechnicalException ex) {
//            LOGGER.error("An error occurs while trying to find all tags", ex);
//            throw new TechnicalManagementException("An error occurs while trying to find all tags", ex);
//        }
//    }
//
//    @Override
//    public List<TagEntity> create(final List<NewTagEntity> tagEntities) {
//        // First we prevent the duplicate tag name
//        final List<String> tagNames = tagEntities.stream()
//                .map(NewTagEntity::getName)
//                .collect(Collectors.toList());
//
//        final Optional<TagEntity> optionalTag = findAll().stream()
//                .filter(tag -> tagNames.contains(tag.getName()))
//                .findAny();
//
//        if (optionalTag.isPresent()) {
//            throw new DuplicateTagNameException(optionalTag.get().getName());
//        }
//
//        final List<TagEntity> savedTags = new ArrayList<>(tagEntities.size());
//        tagEntities.forEach(tagEntity -> {
//            try {
//                Tag tag = convert(tagEntity);
//                savedTags.add(convert(tagRepository.create(tag)));
//                auditService.createPortalAuditLog(
//                        Collections.singletonMap(TAG, tag.getId()),
//                        TAG_CREATED,
//                        new Date(),
//                        null,
//                        tag);
//            } catch (TechnicalException ex) {
//                LOGGER.error("An error occurs while trying to create tag {}", tagEntity.getName(), ex);
//                throw new TechnicalManagementException("An error occurs while trying to create tag " + tagEntity.getName(), ex);
//            }
//        });
//        return savedTags;
//    }
//
//    @Override
//    public List<TagEntity> update(final List<UpdateTagEntity> tagEntities) {
//        final List<TagEntity> savedTags = new ArrayList<>(tagEntities.size());
//        tagEntities.forEach(tagEntity -> {
//            try {
//                Tag tag = convert(tagEntity);
//                Optional<Tag> tagOptional = tagRepository.findById(tag.getId());
//                if (tagOptional.isPresent()) {
//                    savedTags.add(convert(tagRepository.update(tag)));
//                    auditService.createPortalAuditLog(
//                            Collections.singletonMap(TAG, tag.getId()),
//                            TAG_UPDATED,
//                            new Date(),
//                            tagOptional.get(),
//                            tag);
//                }
//            } catch (TechnicalException ex) {
//                LOGGER.error("An error occurs while trying to update tag {}", tagEntity.getName(), ex);
//                throw new TechnicalManagementException("An error occurs while trying to update tag " + tagEntity.getName(), ex);
//            }
//        });
//        return savedTags;
//    }
//
//    @Override
//    public void delete(final String tagId) {
//        try {
//            Optional<Tag> tagOptional = tagRepository.findById(tagId);
//            if (tagOptional.isPresent()) {
//                tagRepository.delete(tagId);
//                // delete all reference on APIs
//                apiService.deleteTagFromAPIs(tagId);
//                auditService.createPortalAuditLog(
//                        Collections.singletonMap(TAG, tagId),
//                        TAG_DELETED,
//                        new Date(),
//                        null,
//                        tagOptional.get());
//            }
//        } catch (TechnicalException ex) {
//            LOGGER.error("An error occurs while trying to delete tag {}", tagId, ex);
//            throw new TechnicalManagementException("An error occurs while trying to delete tag " + tagId, ex);
//        }
//    }
//
//    private Tag convert(final NewTagEntity tagEntity) {
//        final Tag tag = new Tag();
//        tag.setId(IdGenerator.generate(tagEntity.getName()));
//        tag.setName(tagEntity.getName());
//        tag.setDescription(tagEntity.getDescription());
//        return tag;
//    }
//
//    private Tag convert(final UpdateTagEntity tagEntity) {
//        final Tag tag = new Tag();
//        tag.setId(tagEntity.getId());
//        tag.setName(tagEntity.getName());
//        tag.setDescription(tagEntity.getDescription());
//        return tag;
//    }
//
//    private TagEntity convert(final Tag tag) {
//        final TagEntity tagEntity = new TagEntity();
//        tagEntity.setId(tag.getId());
//        tagEntity.setName(tag.getName());
//        tagEntity.setDescription(tag.getDescription());
//        return tagEntity;
//    }
}
