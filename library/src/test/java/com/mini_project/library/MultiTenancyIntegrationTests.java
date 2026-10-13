package com.mini_project.library;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MultiTenancyIntegrationTests {

    @Autowired
    private MockMvc mvc;

    @Test
    void apiRequiresAuthentication() throws Exception {
        mvc.perform(get("/api/books")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/books").header("Authorization", "Bearer not-a-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void signupThenLogin() throws Exception {
        String email = uniqueEmail();
        signup("Riverside Library", email);

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'email':'%s','password':'password123'}", email.toUpperCase())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organization.name").value("Riverside Library"))
                .andExpect(jsonPath("$.user.role").value("OWNER"));

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'email':'%s','password':'wrong-password'}", email)))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'organizationName':'Dup','fullName':'X','email':'%s','password':'password123'}", email)))
                .andExpect(status().isConflict());
    }

    @Test
    void tenantsCannotSeeEachOthersData() throws Exception {
        String tokenA = signup("Library A", uniqueEmail());
        String tokenB = signup("Library B", uniqueEmail());

        long bookId = createBook(tokenA, "Dune", "isbn-dune", 1);
        long memberId = createMember(tokenA, "Alice", uniqueEmail());

        mvc.perform(get("/api/books").header("Authorization", bearer(tokenB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/api/books/" + bookId).header("Authorization", bearer(tokenB)))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/books/" + bookId).header("Authorization", bearer(tokenB)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/members/" + memberId).header("Authorization", bearer(tokenB)))
                .andExpect(status().isNotFound());

        // Org B can't borrow org A's book, even for its own member.
        long memberB = createMember(tokenB, "Bob", uniqueEmail());
        mvc.perform(post("/api/loans").header("Authorization", bearer(tokenB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'bookId':%d,'memberId':%d}", bookId, memberB)))
                .andExpect(status().isNotFound());

        // The same ISBN may exist in two different organizations.
        createBook(tokenB, "Dune", "isbn-dune", 1);
    }

    @Test
    void loansRespectCopiesAndReturn() throws Exception {
        String token = signup("Loan Library", uniqueEmail());
        long bookId = createBook(token, "Emma", "isbn-emma", 1);
        long alice = createMember(token, "Alice", uniqueEmail());
        long bob = createMember(token, "Bob", uniqueEmail());

        String loan = mvc.perform(post("/api/loans").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'bookId':%d,'memberId':%d}", bookId, alice)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.overdue").value(false))
                .andReturn().getResponse().getContentAsString();
        long loanId = ((Number) JsonPath.read(loan, "$.id")).longValue();

        mvc.perform(get("/api/books/" + bookId).header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.availableCopies").value(0));

        mvc.perform(post("/api/loans").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'bookId':%d,'memberId':%d}", bookId, bob)))
                .andExpect(status().isConflict());

        mvc.perform(post("/api/loans/" + loanId + "/return").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
        mvc.perform(post("/api/loans/" + loanId + "/return").header("Authorization", bearer(token)))
                .andExpect(status().isConflict());

        mvc.perform(get("/api/dashboard").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titles").value(1))
                .andExpect(jsonPath("$.members").value(2))
                .andExpect(jsonPath("$.activeLoans").value(0));
    }

    @Test
    void librariansCannotManageTeamOrSettings() throws Exception {
        String owner = signup("Team Library", uniqueEmail());
        String librarianEmail = uniqueEmail();

        mvc.perform(post("/api/team").header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'fullName':'Lib','email':'%s','password':'password123','role':'LIBRARIAN'}",
                                librarianEmail)))
                .andExpect(status().isCreated());

        String librarian = login(librarianEmail);
        mvc.perform(get("/api/books").header("Authorization", bearer(librarian)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/team").header("Authorization", bearer(librarian)))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/organization").header("Authorization", bearer(librarian))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'name':'Hijacked','loanDurationDays':7,'maxActiveLoansPerMember':1}")))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/team").header("Authorization", bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    private String signup(String orgName, String email) throws Exception {
        ResultActions result = mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'organizationName':'%s','fullName':'Owner','email':'%s','password':'password123'}",
                                orgName, email)))
                .andExpect(status().isCreated());
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.token");
    }

    private String login(String email) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'email':'%s','password':'password123'}", email)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    private long createBook(String token, String title, String isbn, int copies) throws Exception {
        String body = mvc.perform(post("/api/books").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'title':'%s','author':'Someone','isbn':'%s','copies':%d}", title, isbn, copies)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private long createMember(String token, String name, String email) throws Exception {
        String body = mvc.perform(post("/api/members").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'name':'%s','email':'%s'}", name, email)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private static String uniqueEmail() {
        return UUID.randomUUID().toString().substring(0, 8) + "@example.com";
    }

    /** Lets test bodies use single quotes instead of escaped double quotes. */
    private static String json(String template, Object... args) {
        return String.format(template, args).replace('\'', '"');
    }
}
