package com.example.beusuarios.security.crypto;

import com.example.beusuarios.service.EncryptionService;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Converter
@Component // Make it a Spring component to inject EncryptionService
public class EmailEncryptor implements AttributeConverter<String, String> {

    private static EncryptionService encryptionService;

    // Static setter for Spring to inject the service
    // This is a common pattern for AttributeConverters that need Spring beans
    @Autowired
    public void setEncryptionService(EncryptionService service) {
        EmailEncryptor.encryptionService = service;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null || encryptionService == null) {
            return attribute;
        }
        return encryptionService.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || encryptionService == null) {
            return dbData;
        }
        return encryptionService.decrypt(dbData);
    }
}
