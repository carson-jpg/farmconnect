package com.farmconnect.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {
    @Override public String convertToDatabaseColumn(String attribute) { return Crypto.encrypt(attribute); }
    @Override public String convertToEntityAttribute(String dbData) { return Crypto.decrypt(dbData); }
}