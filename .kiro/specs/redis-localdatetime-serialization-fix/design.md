# Redis LocalDateTime Serialization Fix - Bugfix Design

## Overview

The application fails to serialize ChargingSession entities to Redis because Jackson's default ObjectMapper lacks support for Java 8 date/time types (LocalDateTime). The fix involves configuring a custom ObjectMapper with the jackson-datatype-jsr310 module to enable proper serialization/deserialization of LocalDateTime fields. This is a minimal configuration change that adds Java 8 date/time support to the existing GenericJackson2JsonRedisSerializer without affecting other serialization behavior.

## Glossary

- **Bug_Condition (C)**: The condition that triggers the bug - when any object containing LocalDateTime fields is serialized to Redis
- **Property (P)**: The desired behavior - LocalDateTime fields should serialize to ISO-8601 format strings and deserialize back to LocalDateTime objects
- **Preservation**: Existing serialization behavior for non-date types (String, Long, BigDecimal, custom objects) must remain unchanged
- **GenericJackson2JsonRedisSerializer**: The Redis value serializer currently used in RedisConfig that wraps Jackson's ObjectMapper
- **JavaTimeModule**: The Jackson module (from jackson-datatype-jsr310) that provides serializers/deserializers for Java 8 date/time types
- **ChargingSession**: The entity in `src/main/java/chat/wisechat/charging/entity/ChargingSession.java` that contains four LocalDateTime fields (startTime, endTime, createTime, updateTime)

## Bug Details

### Fault Condition

The bug manifests when RedisTemplate attempts to serialize any object containing java.time.LocalDateTime fields to Redis. The GenericJackson2JsonRedisSerializer uses Jackson's default ObjectMapper, which does not include the JavaTimeModule required for Java 8 date/time type support.

**Formal Specification:**
```
FUNCTION isBugCondition(input)
  INPUT: input of type Object (being serialized to Redis)
  OUTPUT: boolean
  
  RETURN containsLocalDateTimeField(input)
         AND NOT jacksonObjectMapperHasJavaTimeModule()
         AND serializationAttempted(input)
END FUNCTION
```

### Examples

- **ChargingService.startCharging()**: Attempts to cache ChargingSession with startTime=LocalDateTime.now() → throws SerializationException: "Could not write JSON: Java 8 date/time type `java.time.LocalDateTime` not supported by default"
- **ChargingService.insertGun()**: Attempts to cache ChargingSession with createTime and updateTime populated → throws SerializationException before the cache operation completes
- **ChargingService.stopCharging()**: Attempts to update cached session with endTime=LocalDateTime.now() → throws SerializationException when trying to serialize the updated object
- **Edge case**: Retrieving a ChargingSession from Redis (if it were somehow stored) would fail deserialization, throwing a similar exception when converting JSON back to LocalDateTime

## Expected Behavior

### Preservation Requirements

**Unchanged Behaviors:**
- Serialization of primitive types (Long, Integer, String) must continue to work exactly as before
- Serialization of BigDecimal fields must continue to work exactly as before
- Serialization of custom objects (ChargingDataVO, VehicleBmsDataVO) must continue to work exactly as before
- StringRedisSerializer for keys must remain unchanged
- RedisTemplate bean initialization must succeed without errors

**Scope:**
All inputs that do NOT contain LocalDateTime fields should be completely unaffected by this fix. This includes:
- Long values stored for gun:session and vehicle:session mappings
- ChargingDataVO objects cached in charging:data:* keys
- VehicleBmsDataVO objects cached in bms:data:* keys
- Any other objects that don't use Java 8 date/time types

## Hypothesized Root Cause

Based on the bug description and error message, the root cause is:

1. **Missing Jackson Module**: The GenericJackson2JsonRedisSerializer in RedisConfig uses Jackson's default ObjectMapper, which does not include the jackson-datatype-jsr310 module by default
   - Spring Boot 3.x includes jackson-datatype-jsr310 in the classpath (via spring-boot-starter-web)
   - However, GenericJackson2JsonRedisSerializer's default constructor creates a new ObjectMapper without registering available modules

2. **No Custom ObjectMapper Configuration**: The RedisConfig does not provide a custom ObjectMapper to the GenericJackson2JsonRedisSerializer
   - The no-arg constructor is used: `new GenericJackson2JsonRedisSerializer()`
   - This creates an ObjectMapper without calling `findAndRegisterModules()`

3. **LocalDateTime Serialization Attempt**: When ChargingSession is stored in Redis, Jackson attempts to serialize LocalDateTime fields but has no registered serializer for this type
   - Jackson's default behavior is to throw SerializationException for unsupported types
   - The error message explicitly states "not supported by default"

## Correctness Properties

Property 1: Fault Condition - LocalDateTime Serialization Support

_For any_ object containing LocalDateTime fields that is stored in Redis, the fixed RedisTemplate SHALL successfully serialize the LocalDateTime fields to ISO-8601 format JSON strings and deserialize them back to java.time.LocalDateTime objects without throwing SerializationException.

**Validates: Requirements 2.1, 2.2, 2.3, 2.4**

Property 2: Preservation - Non-Date Type Serialization

_For any_ object that does NOT contain Java 8 date/time types (LocalDateTime, LocalDate, etc.), the fixed RedisTemplate SHALL produce exactly the same serialization behavior as the original configuration, preserving all existing functionality for primitive types, BigDecimal, and custom objects.

**Validates: Requirements 3.1, 3.2, 3.3, 3.4**

## Fix Implementation

### Changes Required

Assuming our root cause analysis is correct:

**File**: `src/main/java/chat/wisechat/charging/config/RedisConfig.java`

**Function**: `redisTemplate(RedisConnectionFactory connectionFactory)`

**Specific Changes**:
1. **Add Jackson Dependency**: Add jackson-datatype-jsr310 dependency to pom.xml (if not already present via spring-boot-starter-web)
   - Verify the dependency is available in the classpath
   - Spring Boot 3.5.11 includes this module by default

2. **Create Custom ObjectMapper**: Instantiate an ObjectMapper with JavaTimeModule registered
   - Call `new ObjectMapper()`
   - Call `objectMapper.findAndRegisterModules()` to auto-discover and register jackson-datatype-jsr310

3. **Configure GenericJackson2JsonRedisSerializer**: Pass the custom ObjectMapper to the serializer constructor
   - Replace `new GenericJackson2JsonRedisSerializer()` with `new GenericJackson2JsonRedisSerializer(objectMapper)`

4. **Maintain Existing Configuration**: Keep all other RedisTemplate configuration unchanged
   - StringRedisSerializer for keys and hash keys
   - The custom JSON serializer for values and hash values
   - Connection factory and afterPropertiesSet() call

## Testing Strategy

### Validation Approach

The testing strategy follows a two-phase approach: first, surface counterexamples that demonstrate the bug on unfixed code, then verify the fix works correctly and preserves existing behavior.

### Exploratory Fault Condition Checking

**Goal**: Surface counterexamples that demonstrate the bug BEFORE implementing the fix. Confirm or refute the root cause analysis. If we refute, we will need to re-hypothesize.

**Test Plan**: Write unit tests that attempt to serialize ChargingSession objects with LocalDateTime fields using the current RedisConfig. Run these tests on the UNFIXED code to observe SerializationException failures and confirm the root cause.

**Test Cases**:
1. **Serialize ChargingSession with startTime**: Create a ChargingSession with startTime=LocalDateTime.now(), attempt to serialize to Redis (will fail on unfixed code with SerializationException)
2. **Serialize ChargingSession with all date fields**: Create a ChargingSession with all four LocalDateTime fields populated, attempt to serialize (will fail on unfixed code)
3. **Deserialize ChargingSession from JSON**: Manually create JSON with LocalDateTime in ISO-8601 format, attempt to deserialize (will fail on unfixed code)
4. **Round-trip test**: Serialize then deserialize a ChargingSession (will fail on unfixed code at serialization step)

**Expected Counterexamples**:
- SerializationException with message "Could not write JSON: Java 8 date/time type `java.time.LocalDateTime` not supported by default"
- Possible causes: missing JavaTimeModule registration, default ObjectMapper without module discovery

### Fix Checking

**Goal**: Verify that for all inputs where the bug condition holds, the fixed function produces the expected behavior.

**Pseudocode:**
```
FOR ALL input WHERE isBugCondition(input) DO
  result := redisTemplate_fixed.opsForValue().set("test:key", input)
  retrieved := redisTemplate_fixed.opsForValue().get("test:key")
  ASSERT result succeeds without exception
  ASSERT retrieved.localDateTimeField equals input.localDateTimeField
END FOR
```

### Preservation Checking

**Goal**: Verify that for all inputs where the bug condition does NOT hold, the fixed function produces the same result as the original function.

**Pseudocode:**
```
FOR ALL input WHERE NOT isBugCondition(input) DO
  ASSERT redisTemplate_original.serialize(input) = redisTemplate_fixed.serialize(input)
END FOR
```

**Testing Approach**: Property-based testing is recommended for preservation checking because:
- It generates many test cases automatically across the input domain
- It catches edge cases that manual unit tests might miss
- It provides strong guarantees that behavior is unchanged for all non-LocalDateTime inputs

**Test Plan**: Observe behavior on UNFIXED code first for non-date objects (Long, String, ChargingDataVO), then write property-based tests capturing that serialization behavior remains identical after the fix.

**Test Cases**:
1. **Long Value Preservation**: Observe that Long sessionId values serialize correctly on unfixed code, then write test to verify identical serialization after fix
2. **String Value Preservation**: Observe that String vehicleId values serialize correctly on unfixed code, then write test to verify identical serialization after fix
3. **BigDecimal Preservation**: Observe that BigDecimal totalPower values serialize correctly on unfixed code, then write test to verify identical serialization after fix
4. **Custom Object Preservation**: Observe that ChargingDataVO and VehicleBmsDataVO serialize correctly on unfixed code, then write test to verify identical serialization after fix

### Unit Tests

- Test serialization of ChargingSession with each LocalDateTime field populated individually
- Test serialization of ChargingSession with all LocalDateTime fields populated
- Test deserialization of ChargingSession from JSON with ISO-8601 formatted dates
- Test round-trip serialization/deserialization preserves LocalDateTime values
- Test that null LocalDateTime fields serialize correctly
- Test edge cases (min/max LocalDateTime values, different time zones if applicable)

### Property-Based Tests

- Generate random ChargingSession objects with random LocalDateTime values and verify successful serialization/deserialization
- Generate random non-date objects (Long, String, BigDecimal) and verify serialization output is identical between original and fixed configurations
- Generate random ChargingDataVO and VehicleBmsDataVO objects and verify preservation of serialization behavior

### Integration Tests

- Test full ChargingService.startCharging() flow with Redis caching enabled
- Test full ChargingService.stopCharging() flow with endTime update in Redis
- Test ChargingService.insertGun() caching with all LocalDateTime fields
- Test retrieval of cached ChargingSession from Redis and verify LocalDateTime fields are correctly deserialized
- Test that MqttMessageHandler continues to cache ChargingDataVO and VehicleBmsDataVO correctly
