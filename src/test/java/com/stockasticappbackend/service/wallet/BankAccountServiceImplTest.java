package com.stockasticappbackend.service.wallet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.stockasticappbackend.config.TestConfig;
import com.stockasticappbackend.dto.wallet.BankAccountResponse;
import com.stockasticappbackend.dto.wallet.LinkBankAccountRequest;
import com.stockasticappbackend.exception.BankAccountNotFoundException;
import com.stockasticappbackend.exception.DuplicateResourceException;
import com.stockasticappbackend.exception.ResourceNotFoundException;
import com.stockasticappbackend.model.entity.AppUser;
import com.stockasticappbackend.model.enums.UserStatus;
import com.stockasticappbackend.repository.ActivityLogRepository;
import com.stockasticappbackend.repository.AppUserRepository;
import com.stockasticappbackend.repository.AutoSellRuleRepository;
import com.stockasticappbackend.repository.BankAccountRepository;

@SpringBootTest
@Import(TestConfig.class)
class BankAccountServiceImplTest {

    @Autowired
    private BankAccountService bankAccountService;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private BankAccountRepository bankAccountRepository;

    @Autowired
    private AutoSellRuleRepository autoSellRuleRepository;

    @Autowired
    private ActivityLogRepository activityLogRepository;

    private AppUser testUser;

    @BeforeEach
    void setUp() {
        autoSellRuleRepository.deleteAll();
        bankAccountRepository.deleteAll();
        activityLogRepository.deleteAll();
        userRepository.deleteAll();

        testUser = new AppUser();
        testUser.setEmail("bank@example.com");
        testUser.setName("Bank User");
        testUser.setPasswordHash("hash");
        testUser.setUserStatus(UserStatus.ACTIVE);
        testUser = userRepository.save(testUser);
    }

    @Test
    void linkBankAccount_FirstAccount_ShouldBePrimary() {
        LinkBankAccountRequest request = new LinkBankAccountRequest();
        request.setBankName("HDFC Bank");
        request.setAccountNumber("123456789012");
        request.setIfscCode("HDFC0001234");
        request.setHolderName("Bank User");
        BankAccountResponse response = bankAccountService.linkBankAccount("bank@example.com", request);
        assertNotNull(response);
        assertEquals("HDFC Bank", response.getBankName());
        assertTrue(response.getIsPrimary());
        assertTrue(response.getIsVerified());
    }

    @Test
    void linkBankAccount_SecondAccount_ShouldNotBePrimary() {
        LinkBankAccountRequest first = new LinkBankAccountRequest();
        first.setBankName("HDFC Bank");
        first.setAccountNumber("123456789012");
        first.setIfscCode("HDFC0001234");
        first.setHolderName("Bank User");
        bankAccountService.linkBankAccount("bank@example.com", first);
        LinkBankAccountRequest second = new LinkBankAccountRequest();
        second.setBankName("SBI");
        second.setAccountNumber("987654321098");
        second.setIfscCode("SBIN0005678");
        second.setHolderName("Bank User");
        BankAccountResponse response = bankAccountService.linkBankAccount("bank@example.com", second);
        assertNotNull(response);
        assertFalse(response.getIsPrimary());
    }

    @Test
    void linkBankAccount_DuplicateAccount_ShouldThrowException() {
        LinkBankAccountRequest request = new LinkBankAccountRequest();
        request.setBankName("HDFC Bank");
        request.setAccountNumber("123456789012");
        request.setIfscCode("HDFC0001234");
        request.setHolderName("Bank User");
        bankAccountService.linkBankAccount("bank@example.com", request);

        LinkBankAccountRequest duplicate = new LinkBankAccountRequest();
        duplicate.setBankName("HDFC Bank");
        duplicate.setAccountNumber("123456789012");
        duplicate.setIfscCode("HDFC0001234");
        duplicate.setHolderName("Bank User");
        assertThrows(DuplicateResourceException.class, () -> {
            bankAccountService.linkBankAccount("bank@example.com", duplicate);
        });
    }

    @Test
    void getBankAccounts_ShouldReturnAllAccounts() {
        LinkBankAccountRequest r1 = new LinkBankAccountRequest();
        r1.setBankName("HDFC");
        r1.setAccountNumber("111111111111");
        r1.setIfscCode("HDFC0001111");
        r1.setHolderName("User");
        bankAccountService.linkBankAccount("bank@example.com", r1);

        LinkBankAccountRequest r2 = new LinkBankAccountRequest();
        r2.setBankName("SBI");
        r2.setAccountNumber("222222222222");
        r2.setIfscCode("SBIN0002222");
        r2.setHolderName("User");
        bankAccountService.linkBankAccount("bank@example.com", r2);
        List<BankAccountResponse> accounts = bankAccountService.getBankAccounts("bank@example.com");
        assertEquals(2, accounts.size());
    }

    @Test
    void getPrimaryBankAccount_ShouldReturnPrimary() {
        LinkBankAccountRequest request = new LinkBankAccountRequest();
        request.setBankName("HDFC Bank");
        request.setAccountNumber("123456789012");
        request.setIfscCode("HDFC0001234");
        request.setHolderName("Bank User");
        bankAccountService.linkBankAccount("bank@example.com", request);
        BankAccountResponse primary = bankAccountService.getPrimaryBankAccount("bank@example.com");
        assertNotNull(primary);
        assertTrue(primary.getIsPrimary());
        assertEquals("HDFC Bank", primary.getBankName());
    }

    @Test
    void getPrimaryBankAccount_NoAccounts_ShouldReturnNull() {
        BankAccountResponse primary = bankAccountService.getPrimaryBankAccount("bank@example.com");
        assertNull(primary);
    }

    @Test
    void deleteBankAccount_PrimaryAccount_ShouldPromoteAnother() {
        LinkBankAccountRequest r1 = new LinkBankAccountRequest();
        r1.setBankName("HDFC");
        r1.setAccountNumber("111111111111");
        r1.setIfscCode("HDFC0001111");
        r1.setHolderName("User");
        BankAccountResponse first = bankAccountService.linkBankAccount("bank@example.com", r1);

        LinkBankAccountRequest r2 = new LinkBankAccountRequest();
        r2.setBankName("SBI");
        r2.setAccountNumber("222222222222");
        r2.setIfscCode("SBIN0002222");
        r2.setHolderName("User");
        bankAccountService.linkBankAccount("bank@example.com", r2);
        bankAccountService.deleteBankAccount("bank@example.com", first.getId());
        List<BankAccountResponse> remaining = bankAccountService.getBankAccounts("bank@example.com");
        assertEquals(1, remaining.size());
        assertTrue(remaining.get(0).getIsPrimary());
    }

    @Test
    void deleteBankAccount_NotFound_ShouldThrowException() {
        assertThrows(BankAccountNotFoundException.class, () -> {
            bankAccountService.deleteBankAccount("bank@example.com", 999999L);
        });
    }

    @Test
    void setPrimaryBankAccount_ShouldSwitchPrimary() {
        LinkBankAccountRequest r1 = new LinkBankAccountRequest();
        r1.setBankName("HDFC");
        r1.setAccountNumber("111111111111");
        r1.setIfscCode("HDFC0001111");
        r1.setHolderName("User");
        bankAccountService.linkBankAccount("bank@example.com", r1);

        LinkBankAccountRequest r2 = new LinkBankAccountRequest();
        r2.setBankName("SBI");
        r2.setAccountNumber("222222222222");
        r2.setIfscCode("SBIN0002222");
        r2.setHolderName("User");
        BankAccountResponse second = bankAccountService.linkBankAccount("bank@example.com", r2);
        BankAccountResponse newPrimary = bankAccountService.setPrimaryBankAccount("bank@example.com", second.getId());
        assertTrue(newPrimary.getIsPrimary());
        assertEquals("SBI", newPrimary.getBankName());
    }

    @Test
    void linkBankAccount_UserNotFound_ShouldThrowException() {
        LinkBankAccountRequest request = new LinkBankAccountRequest();
        request.setBankName("Test");
        request.setAccountNumber("123456789012");
        request.setIfscCode("TEST0001234");
        request.setHolderName("No One");
        assertThrows(ResourceNotFoundException.class, () -> {
            bankAccountService.linkBankAccount("nonexistent@example.com", request);
        });
    }
}

