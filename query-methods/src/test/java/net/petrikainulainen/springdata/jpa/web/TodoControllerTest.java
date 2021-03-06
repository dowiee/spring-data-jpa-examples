package net.petrikainulainen.springdata.jpa.web;

import com.nitorcreations.junit.runners.NestedRunner;
import net.petrikainulainen.springdata.jpa.todo.TestUtil;
import net.petrikainulainen.springdata.jpa.todo.TodoCrudService;
import net.petrikainulainen.springdata.jpa.todo.TodoDTO;
import net.petrikainulainen.springdata.jpa.todo.TodoDTOBuilder;
import net.petrikainulainen.springdata.jpa.todo.TodoNotFoundException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

import static net.petrikainulainen.springdata.jpa.todo.TodoDTOAssert.assertThatTodoDTO;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Petri Kainulainen
 */
@RunWith(NestedRunner.class)
public class TodoControllerTest {

    private static final Locale CURRENT_LOCALE = Locale.US;
    private static final String CREATION_TIME = "2014-12-24T22:28:39+02:00";
    private static final String DESCRIPTION = "description";

    private static final String ERROR_MESSAGE_KEY_MISSING_TITLE = "NotEmpty.todoDTO.title";
    private static final String ERROR_MESSAGE_KEY_TODO_ENTRY_NOT_FOUND = "error.todo.entry.not.found";
    private static final String ERROR_MESSAGE_KEY_TOO_LONG_DESCRIPTION = "Size.todoDTO.description";
    private static final String ERROR_MESSAGE_KEY_TOO_LONG_TITLE = "Size.todoDTO.title";

    private static final Long ID = 1L;
    private static final String MODIFICATION_TIME = "2014-12-24T14:28:39+02:00";
    private static final String TITLE = "title";

    private MockMvc mockMvc;

    private TodoCrudService crudService;

    private StaticMessageSource messageSource;

    @Before
    public void setUp() {
        crudService = mock(TodoCrudService.class);

        messageSource = new StaticMessageSource();
        messageSource.setUseCodeAsDefaultMessage(true);

        mockMvc = MockMvcBuilders.standaloneSetup(new TodoController(crudService))
                .setHandlerExceptionResolvers(WebTestConfig.restErrorHandler(messageSource))
                .setLocaleResolver(WebTestConfig.fixedLocaleResolver(CURRENT_LOCALE))
                .setMessageConverters(WebTestConfig.jacksonDateTimeConverter())
                .setValidator(WebTestConfig.validator())
                .build();
    }

    public class Create {

        public class WhenTodoEntryIsNotValid {

            public class WhenTodoEntryIsEmpty {

                @Test
                public void shouldReturnResponseStatusBadRequest() throws Exception {
                    TodoDTO emptyTodoEntry = new TodoDTO();

                    mockMvc.perform(post("/api/todo")
                                    .contentType(WebTestConstants.APPLICATION_JSON_UTF8)
                                    .content(WebTestUtil.convertObjectToJsonBytes(emptyTodoEntry))
                    )
                            .andExpect(status().isBadRequest());
                }

                @Test
                public void shouldReturnValidationErrorAboutMissingTitleAsJson() throws Exception {
                    TodoDTO emptyTodoEntry = new TodoDTO();

                    mockMvc.perform(post("/api/todo")
                                    .contentType(WebTestConstants.APPLICATION_JSON_UTF8)
                                    .content(WebTestUtil.convertObjectToJsonBytes(emptyTodoEntry))
                    )
                            .andExpect(content().contentType(WebTestConstants.APPLICATION_JSON_UTF8))
                            .andExpect(jsonPath("$.code", is(WebTestConstants.ERROR_CODE_VALIDATION_FAILED)))
                            .andExpect(jsonPath("$.fieldErrors", hasSize(1)))
                            .andExpect(jsonPath("$.fieldErrors[0].field", is(WebTestConstants.FIELD_NAME_TITLE)))
                            .andExpect(jsonPath("$.fieldErrors[0].message", is(ERROR_MESSAGE_KEY_MISSING_TITLE)));
                }

                @Test
                public void shouldNotCreateNewTodoEntry() throws Exception {
                    TodoDTO emptyTodoEntry = new TodoDTO();

                    mockMvc.perform(post("/api/todo")
                                    .contentType(WebTestConstants.APPLICATION_JSON_UTF8)
                                    .content(WebTestUtil.convertObjectToJsonBytes(emptyTodoEntry))
                    );

                    verifyZeroInteractions(crudService);
                }
            }

            public class WhenTitleAndDescriptionAreTooLong {

                @Test
                public void shouldReturnResponseStatusBadRequest() throws Exception {
                    String tooLongDescription = TestUtil.createStringWithLength(WebTestConstants.MAX_LENGTH_DESCRIPTION + 1);
                    String tooLongTitle = TestUtil.createStringWithLength(WebTestConstants.MAX_LENGTH_TITLE + 1);

                    TodoDTO newTodoEntry = new TodoDTOBuilder()
                            .description(tooLongDescription)
                            .title(tooLongTitle)
                            .build();

                    mockMvc.perform(post("/api/todo")
                                    .contentType(WebTestConstants.APPLICATION_JSON_UTF8)
                                    .content(WebTestUtil.convertObjectToJsonBytes(newTodoEntry))
                    )
                            .andExpect(status().isBadRequest());
                }

                @Test
                public void shouldReturnValidationErrorsAboutTitleAndDescriptionAsJson() throws Exception {
                    String tooLongDescription = TestUtil.createStringWithLength(WebTestConstants.MAX_LENGTH_DESCRIPTION + 1);
                    String tooLongTitle = TestUtil.createStringWithLength(WebTestConstants.MAX_LENGTH_TITLE + 1);

                    TodoDTO newTodoEntry = new TodoDTOBuilder()
                            .description(tooLongDescription)
                            .title(tooLongTitle)
                            .build();

                    mockMvc.perform(post("/api/todo")
                                    .contentType(WebTestConstants.APPLICATION_JSON_UTF8)
                                    .content(WebTestUtil.convertObjectToJsonBytes(newTodoEntry))
                    )
                            .andExpect(content().contentType(WebTestConstants.APPLICATION_JSON_UTF8))
                            .andExpect(jsonPath("$.code", is(WebTestConstants.ERROR_CODE_VALIDATION_FAILED)))
                            .andExpect(jsonPath("$.fieldErrors", hasSize(2)))
                            .andExpect(jsonPath("$.fieldErrors[*].field", containsInAnyOrder(
                                    WebTestConstants.FIELD_NAME_DESCRIPTION,
                                    WebTestConstants.FIELD_NAME_TITLE
                            )))
                            .andExpect(jsonPath("$.fieldErrors[*].message", containsInAnyOrder(
                                    ERROR_MESSAGE_KEY_TOO_LONG_DESCRIPTION,
                                    ERROR_MESSAGE_KEY_TOO_LONG_TITLE
                            )));
                }

                @Test
                public void shouldNotCreateNewTodoEntry() throws Exception {
                    String tooLongDescription = TestUtil.createStringWithLength(WebTestConstants.MAX_LENGTH_DESCRIPTION + 1);
                    String tooLongTitle = TestUtil.createStringWithLength(WebTestConstants.MAX_LENGTH_TITLE + 1);

                    TodoDTO newTodoEntry = new TodoDTOBuilder()
                            .description(tooLongDescription)
                            .title(tooLongTitle)
                            .build();

                    mockMvc.perform(post("/api/todo")
                                    .contentType(WebTestConstants.APPLICATION_JSON_UTF8)
                                    .content(WebTestUtil.convertObjectToJsonBytes(newTodoEntry))
                    );

                    verifyZeroInteractions(crudService);
                }
            }
        }

        public class WhenTodoEntryIsValid {

            public class WhenMaxLengthTitleAndDescriptionAreGiven {

                @Test
                public void shouldReturnResponseStatusCreated() throws Exception {
                    String maxLengthDescription = TestUtil.createStringWithLength(WebTestConstants.MAX_LENGTH_DESCRIPTION);
                    String maxLengthTitle = TestUtil.createStringWithLength(WebTestConstants.MAX_LENGTH_TITLE);

                    TodoDTO newTodoEntry = new TodoDTOBuilder()
                            .description(maxLengthDescription)
                            .title(maxLengthTitle)
                            .build();

                    mockMvc.perform(post("/api/todo")
                                    .contentType(WebTestConstants.APPLICATION_JSON_UTF8)
                                    .content(WebTestUtil.convertObjectToJsonBytes(newTodoEntry))
                    )
                            .andExpect(status().isCreated());
                }

                @Test
                public void shouldReturnCreatedTodoEntryAsJson() throws Exception {
                    String maxLengthDescription = TestUtil.createStringWithLength(WebTestConstants.MAX_LENGTH_DESCRIPTION);
                    String maxLengthTitle = TestUtil.createStringWithLength(WebTestConstants.MAX_LENGTH_TITLE);

                    TodoDTO newTodoEntry = new TodoDTOBuilder()
                            .description(maxLengthDescription)
                            .title(maxLengthTitle)
                            .build();

                    TodoDTO created = new TodoDTOBuilder()
                            .creationTime(CREATION_TIME)
                            .description(maxLengthDescription)
                            .id(ID)
                            .modificationTime(MODIFICATION_TIME)
                            .title(maxLengthTitle)
                            .build();
                    given(crudService.create(isA(TodoDTO.class))).willReturn(created);

                    mockMvc.perform(post("/api/todo")
                                    .contentType(WebTestConstants.APPLICATION_JSON_UTF8)
                                    .content(WebTestUtil.convertObjectToJsonBytes(newTodoEntry))
                    )
                            .andExpect(content().contentType(WebTestConstants.APPLICATION_JSON_UTF8))
                            .andExpect(jsonPath("$.creationTime", is(CREATION_TIME)))
                            .andExpect(jsonPath("$.description", is(maxLengthDescription)))
                            .andExpect(jsonPath("$.id", is(ID.intValue())))
                            .andExpect(jsonPath("$.modificationTime", is(MODIFICATION_TIME)))
                            .andExpect(jsonPath("$.title", is(maxLengthTitle)));
                }

                @Test
                public void shouldCreateNewTodoEntryWithCorrectInformation() throws Exception {
                    String maxLengthDescription = TestUtil.createStringWithLength(WebTestConstants.MAX_LENGTH_DESCRIPTION);
                    String maxLengthTitle = TestUtil.createStringWithLength(WebTestConstants.MAX_LENGTH_TITLE);

                    TodoDTO newTodoEntry = new TodoDTOBuilder()
                            .description(maxLengthDescription)
                            .title(maxLengthTitle)
                            .build();

                    mockMvc.perform(post("/api/todo")
                                    .contentType(WebTestConstants.APPLICATION_JSON_UTF8)
                                    .content(WebTestUtil.convertObjectToJsonBytes(newTodoEntry))
                    );

                    ArgumentCaptor<TodoDTO> createdTodoEntryArgument = ArgumentCaptor.forClass(TodoDTO.class);
                    verify(crudService, times(1)).create(createdTodoEntryArgument.capture());

                    TodoDTO created = createdTodoEntryArgument.getValue();

                    assertThatTodoDTO(created)
                            .hasDescription(maxLengthDescription)
                            .hasTitle(maxLengthTitle)
                            .hasNoCreationTime()
                            .hasNoId()
                            .hasNoModificationTime();
                }
            }
        }
    }

    public class Delete {

        public class WhenTodoEntryIsNotFound {

            @Test
            public void shouldReturnResponseStatusNotFound() throws Exception {
                given(crudService.delete(ID)).willThrow(new TodoNotFoundException(ID));

                mockMvc.perform(delete("/api/todo/{id}", ID))
                        .andExpect(status().isNotFound());
            }

            @Test
            public void shouldReturnErrorMessageAsJson() throws Exception {
                given(crudService.delete(ID)).willThrow(new TodoNotFoundException(ID));

                mockMvc.perform(delete("/api/todo/{id}", ID))
                        .andExpect(content().contentType(WebTestConstants.APPLICATION_JSON_UTF8))
                        .andExpect(jsonPath("$.code", is(WebTestConstants.ERROR_CODE_TODO_ENTRY_NOT_FOUND)))
                        .andExpect(jsonPath("message", is(ERROR_MESSAGE_KEY_TODO_ENTRY_NOT_FOUND)));
            }
        }

        public class WhenTodoEntryIsFound {

            @Test
            public void shouldReturnResponseStatusOk() throws Exception {
                TodoDTO deleted = new TodoDTO();

                given(crudService.delete(ID)).willReturn(deleted);

                mockMvc.perform(delete("/api/todo/{id}", ID))
                        .andExpect(status().isOk());
            }

            @Test
            public void shouldReturnInformationOfDeletedTodoEntryAsJson() throws Exception {
                TodoDTO deleted = new TodoDTOBuilder()
                        .creationTime(CREATION_TIME)
                        .description(DESCRIPTION)
                        .id(ID)
                        .modificationTime(MODIFICATION_TIME)
                        .title(TITLE)
                        .build();

                given(crudService.delete(ID)).willReturn(deleted);

                mockMvc.perform(delete("/api/todo/{id}", ID))
                        .andExpect(content().contentType(WebTestConstants.APPLICATION_JSON_UTF8))
                        .andExpect(jsonPath("$.creationTime", is(CREATION_TIME)))
                        .andExpect(jsonPath("$.description", is(DESCRIPTION)))
                        .andExpect(jsonPath("$.id", is(ID.intValue())))
                        .andExpect(jsonPath("$.modificationTime", is(MODIFICATION_TIME)))
                        .andExpect(jsonPath("$.title", is(TITLE)));
            }
        }
    }

    public class FindAll {

        @Test
        public void shouldReturnResponseStatusOk() throws Exception {
            mockMvc.perform(get("/api/todo"))
                    .andExpect(status().isOk());
        }

        public class WhenNoTodoEntriesAreFound {

            @Test
            public void shouldReturnEmptyListAsJson() throws Exception {
                given(crudService.findAll()).willReturn(new ArrayList<>());

                mockMvc.perform(get("/api/todo"))
                        .andExpect(content().contentType(WebTestConstants.APPLICATION_JSON_UTF8))
                        .andExpect(jsonPath("$", hasSize(0)));
            }
        }

        public class WhenOneTodoEntryIsFound {

            @Test
            public void shouldReturnOneTodoEntryAsJson() throws Exception {
                TodoDTO found = new TodoDTOBuilder()
                        .creationTime(CREATION_TIME)
                        .description(DESCRIPTION)
                        .id(ID)
                        .modificationTime(MODIFICATION_TIME)
                        .title(TITLE)
                        .build();

                given(crudService.findAll()).willReturn(Arrays.asList(found));

                mockMvc.perform(get("/api/todo"))
                        .andExpect(content().contentType(WebTestConstants.APPLICATION_JSON_UTF8))
                        .andExpect(jsonPath("$", hasSize(1)))
                        .andExpect(jsonPath("$[0].creationTime", is(CREATION_TIME)))
                        .andExpect(jsonPath("$[0].description", is(DESCRIPTION)))
                        .andExpect(jsonPath("$[0].id", is(ID.intValue())))
                        .andExpect(jsonPath("$[0].modificationTime", is(MODIFICATION_TIME)))
                        .andExpect(jsonPath("$[0].title", is(TITLE)));
            }
        }
    }

    public class FindById {

        public class WhenTodoEntryIsNotFound {

            @Test
            public void shouldReturnResponseStatusNotFound() throws Exception {
                given(crudService.findById(ID)).willThrow(new TodoNotFoundException(ID));

                mockMvc.perform(get("/api/todo/{id}", ID))
                        .andExpect(status().isNotFound());
            }

            @Test
            public void shouldReturnErrorMessageAsJson() throws Exception {
                given(crudService.findById(ID)).willThrow(new TodoNotFoundException(ID));

                mockMvc.perform(get("/api/todo/{id}", ID))
                        .andExpect(content().contentType(WebTestConstants.APPLICATION_JSON_UTF8))
                        .andExpect(jsonPath("$.code", is(WebTestConstants.ERROR_CODE_TODO_ENTRY_NOT_FOUND)))
                        .andExpect(jsonPath("message", is(ERROR_MESSAGE_KEY_TODO_ENTRY_NOT_FOUND)));
            }
        }

        public class WhenTodoEntryIsFound {

            @Test
            public void shouldReturnResponseStatusOk() throws Exception {
                TodoDTO found = new TodoDTO();

                given(crudService.findById(ID)).willReturn(found);

                mockMvc.perform(get("/api/todo/{id}", ID))
                        .andExpect(status().isOk());
            }

            @Test
            public void shouldReturnInformationOfFoundTodoEntryAsJson() throws Exception {
                TodoDTO found = new TodoDTOBuilder()
                        .creationTime(CREATION_TIME)
                        .description(DESCRIPTION)
                        .id(ID)
                        .modificationTime(MODIFICATION_TIME)
                        .title(TITLE)
                        .build();

                given(crudService.findById(ID)).willReturn(found);

                mockMvc.perform(get("/api/todo/{id}", ID))
                        .andExpect(content().contentType(WebTestConstants.APPLICATION_JSON_UTF8))
                        .andExpect(jsonPath("$.creationTime", is(CREATION_TIME)))
                        .andExpect(jsonPath("$.description", is(DESCRIPTION)))
                        .andExpect(jsonPath("$.id", is(ID.intValue())))
                        .andExpect(jsonPath("$.modificationTime", is(MODIFICATION_TIME)))
                        .andExpect(jsonPath("$.title", is(TITLE)));
            }
        }
    }

    public class Update {

        public class WhenTodoEntryIsNotFound {

            @Test
            public void shouldReturnResponseStatusNotFound() throws Exception {
                TodoDTO updatedTodoEntry = new TodoDTOBuilder()
                        .id(ID)
                        .build();

                given(crudService.update(isA(TodoDTO.class))).willThrow(new TodoNotFoundException(ID));

                mockMvc.perform(put("/api/todo/{id}", ID)
                                .contentType(WebTestConstants.APPLICATION_JSON_UTF8)
                                .content(WebTestUtil.convertObjectToJsonBytes(updatedTodoEntry))
                )
                        .andExpect(status().isNotFound());
            }

            @Test
            public void shouldReturnErrorMessageAsJson() throws Exception {
                TodoDTO updatedTodoEntry = new TodoDTOBuilder()
                        .id(ID)
                        .build();

                given(crudService.update(isA(TodoDTO.class))).willThrow(new TodoNotFoundException(ID));

                mockMvc.perform(put("/api/todo/{id}", ID)
                                .contentType(WebTestConstants.APPLICATION_JSON_UTF8)
                                .content(WebTestUtil.convertObjectToJsonBytes(updatedTodoEntry))
                )
                        .andExpect(content().contentType(WebTestConstants.APPLICATION_JSON_UTF8))
                        .andExpect(jsonPath("$.code", is(WebTestConstants.ERROR_CODE_TODO_ENTRY_NOT_FOUND)))
                        .andExpect(jsonPath("message", is(ERROR_MESSAGE_KEY_TODO_ENTRY_NOT_FOUND)));
            }
        }

        public class WhenTodoEntryIsFound {

            public class WhenTodoEntryIsNotValid {

                public class WhenTitleAndDescriptionAreMissing {

                    @Test
                    public void shouldReturnResponseStatusBadRequest() throws Exception {
                        TodoDTO updatedTodoEntry = new TodoDTOBuilder()
                                .description(null)
                                .id(ID)
                                .title(null)
                                .build();

                        mockMvc.perform(put("/api/todo/{id}", ID)
                                        .contentType(WebTestConstants.APPLICATION_JSON_UTF8)
                                        .content(WebTestUtil.convertObjectToJsonBytes(updatedTodoEntry))
                        )
                                .andExpect(status().isBadRequest());
                    }

                    @Test
                    public void shouldReturnValidationErrorAboutMissingTitleAsJson() throws Exception {
                        TodoDTO updatedTodoEntry = new TodoDTOBuilder()
                                .description(null)
                                .id(ID)
                                .title(null)
                                .build();

                        mockMvc.perform(put("/api/todo/{id}", ID)
                                        .contentType(WebTestConstants.APPLICATION_JSON_UTF8)
                                        .content(WebTestUtil.convertObjectToJsonBytes(updatedTodoEntry))
                        )
                                .andExpect(content().contentType(WebTestConstants.APPLICATION_JSON_UTF8))
                                .andExpect(jsonPath("$.code", is(WebTestConstants.ERROR_CODE_VALIDATION_FAILED)))
                                .andExpect(jsonPath("$.fieldErrors", hasSize(1)))
                                .andExpect(jsonPath("$.fieldErrors[0].field", is(WebTestConstants.FIELD_NAME_TITLE)))
                                .andExpect(jsonPath("$.fieldErrors[0].message", is(ERROR_MESSAGE_KEY_MISSING_TITLE)));
                    }

                    @Test
                    public void shouldNotUpdateTodoEntry() throws Exception {
                        TodoDTO updatedTodoEntry = new TodoDTOBuilder()
                                .description(null)
                                .id(ID)
                                .title(null)
                                .build();

                        mockMvc.perform(put("/api/todo/{id}", ID)
                                        .contentType(WebTestConstants.APPLICATION_JSON_UTF8)
                                        .content(WebTestUtil.convertObjectToJsonBytes(updatedTodoEntry))
                        );

                        verifyZeroInteractions(crudService);
                    }
                }

                public class WhenTitleAndDescriptionAreTooLong {

                    @Test
                    public void shouldReturnResponseStatusBadRequest() throws Exception {
                        String tooLongDescription = TestUtil.createStringWithLength(WebTestConstants.MAX_LENGTH_DESCRIPTION + 1);
                        String tooLongTitle = TestUtil.createStringWithLength(WebTestConstants.MAX_LENGTH_TITLE + 1);

                        TodoDTO updatedTodoEntry = new TodoDTOBuilder()
                                .description(tooLongDescription)
                                .id(ID)
                                .title(tooLongTitle)
                                .build();

                        mockMvc.perform(put("/api/todo/{id}", ID)
                                        .contentType(WebTestConstants.APPLICATION_JSON_UTF8)
                                        .content(WebTestUtil.convertObjectToJsonBytes(updatedTodoEntry))
                        )
                                .andExpect(status().isBadRequest());
                    }

                    @Test
                    public void shouldReturnValidationErrorsAboutTitleAndDescriptionAsJson() throws Exception {
                        String tooLongDescription = TestUtil.createStringWithLength(WebTestConstants.MAX_LENGTH_DESCRIPTION + 1);
                        String tooLongTitle = TestUtil.createStringWithLength(WebTestConstants.MAX_LENGTH_TITLE + 1);

                        TodoDTO updatedTodoEntry = new TodoDTOBuilder()
                                .description(tooLongDescription)
                                .id(ID)
                                .title(tooLongTitle)
                                .build();

                        mockMvc.perform(put("/api/todo/{id}", ID)
                                        .contentType(WebTestConstants.APPLICATION_JSON_UTF8)
                                        .content(WebTestUtil.convertObjectToJsonBytes(updatedTodoEntry))
                        )
                                .andExpect(content().contentType(WebTestConstants.APPLICATION_JSON_UTF8))
                                .andExpect(jsonPath("$.code", is(WebTestConstants.ERROR_CODE_VALIDATION_FAILED)))
                                .andExpect(jsonPath("$.fieldErrors", hasSize(2)))
                                .andExpect(jsonPath("$.fieldErrors[*].field", containsInAnyOrder(
                                        WebTestConstants.FIELD_NAME_DESCRIPTION,
                                        WebTestConstants.FIELD_NAME_TITLE
                                )))
                                .andExpect(jsonPath("$.fieldErrors[*].message", containsInAnyOrder(
                                        ERROR_MESSAGE_KEY_TOO_LONG_DESCRIPTION,
                                        ERROR_MESSAGE_KEY_TOO_LONG_TITLE
                                )));
                    }

                    @Test
                    public void shouldNotUpdateTodoEntry() throws Exception {
                        String tooLongDescription = TestUtil.createStringWithLength(WebTestConstants.MAX_LENGTH_DESCRIPTION + 1);
                        String tooLongTitle = TestUtil.createStringWithLength(WebTestConstants.MAX_LENGTH_TITLE + 1);

                        TodoDTO updatedTodoEntry = new TodoDTOBuilder()
                                .description(tooLongDescription)
                                .id(ID)
                                .title(tooLongTitle)
                                .build();

                        mockMvc.perform(put("/api/todo/{id}", ID)
                                        .contentType(WebTestConstants.APPLICATION_JSON_UTF8)
                                        .content(WebTestUtil.convertObjectToJsonBytes(updatedTodoEntry))
                        );

                        verifyZeroInteractions(crudService);
                    }
                }
            }

            public class WhenTodoEntryIsValid {

                public class WhenMaxLengthTitleAndDescriptionAreGiven {

                    @Test
                    public void shouldReturnResponseStatusOk() throws Exception {
                        String maxLengthDescription = TestUtil.createStringWithLength(WebTestConstants.MAX_LENGTH_DESCRIPTION);
                        String maxLengthTitle = TestUtil.createStringWithLength(WebTestConstants.MAX_LENGTH_TITLE);

                        TodoDTO updatedTodoEntry = new TodoDTOBuilder()
                                .description(maxLengthDescription)
                                .id(ID)
                                .title(maxLengthTitle)
                                .build();

                        mockMvc.perform(put("/api/todo/{id}", ID)
                                        .contentType(WebTestConstants.APPLICATION_JSON_UTF8)
                                        .content(WebTestUtil.convertObjectToJsonBytes(updatedTodoEntry))
                        )
                                .andExpect(status().isOk());
                    }

                    @Test
                    public void shouldReturnInformationOfUpdatedTodoEntryAsJson() throws Exception {
                        String maxLengthDescription = TestUtil.createStringWithLength(WebTestConstants.MAX_LENGTH_DESCRIPTION);
                        String maxLengthTitle = TestUtil.createStringWithLength(WebTestConstants.MAX_LENGTH_TITLE);

                        TodoDTO updatedTodoEntry = new TodoDTOBuilder()
                                .description(maxLengthDescription)
                                .id(ID)
                                .title(maxLengthTitle)
                                .build();

                        TodoDTO updated = new TodoDTOBuilder()
                                .creationTime(CREATION_TIME)
                                .description(maxLengthDescription)
                                .id(ID)
                                .modificationTime(MODIFICATION_TIME)
                                .title(maxLengthTitle)
                                .build();
                        given(crudService.update(isA(TodoDTO.class))).willReturn(updated);

                        mockMvc.perform(put("/api/todo/{id}", ID)
                                        .contentType(WebTestConstants.APPLICATION_JSON_UTF8)
                                        .content(WebTestUtil.convertObjectToJsonBytes(updatedTodoEntry))
                        )
                                .andExpect(content().contentType(WebTestConstants.APPLICATION_JSON_UTF8))
                                .andExpect(jsonPath("$.creationTime", is(CREATION_TIME)))
                                .andExpect(jsonPath("$.description", is(maxLengthDescription)))
                                .andExpect(jsonPath("$.id", is(ID.intValue())))
                                .andExpect(jsonPath("$.modificationTime", is(MODIFICATION_TIME)))
                                .andExpect(jsonPath("$.title", is(maxLengthTitle)));
                    }

                    @Test
                    public void shouldUpdateTodoEntryWithCorrectInformation() throws Exception {
                        String maxLengthDescription = TestUtil.createStringWithLength(WebTestConstants.MAX_LENGTH_DESCRIPTION);
                        String maxLengthTitle = TestUtil.createStringWithLength(WebTestConstants.MAX_LENGTH_TITLE);

                        TodoDTO updatedTodoEntry = new TodoDTOBuilder()
                                .description(maxLengthDescription)
                                .id(ID)
                                .title(maxLengthTitle)
                                .build();

                        mockMvc.perform(put("/api/todo/{id}", ID)
                                        .contentType(WebTestConstants.APPLICATION_JSON_UTF8)
                                        .content(WebTestUtil.convertObjectToJsonBytes(updatedTodoEntry))
                        );

                        ArgumentCaptor<TodoDTO> updatedArgument = ArgumentCaptor.forClass(TodoDTO.class);
                        verify(crudService, times(1)).update(updatedArgument.capture());

                        TodoDTO updated = updatedArgument.getValue();
                        assertThatTodoDTO(updated)
                                .hasDescription(maxLengthDescription)
                                .hasId(ID)
                                .hasTitle(maxLengthTitle)
                                .hasNoCreationTime()
                                .hasNoModificationTime();
                    }
                }
            }
        }
    }
}
