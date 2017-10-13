package org.dlearn.helsinki.skeleton.resource;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.dlearn.helsinki.skeleton.model.Student;
import org.dlearn.helsinki.skeleton.model.StudentThemeAverage;
import org.dlearn.helsinki.skeleton.service.TeacherClassStudentService;
import org.dlearn.helsinki.skeleton.service.TeacherStudentService;

public class TeacherClassStudentResource {

    TeacherClassStudentService service = new TeacherClassStudentService();
    TeacherStudentService teacherStudentService = new TeacherStudentService();

    // GET student info /{student_id}/

    // GET student info /{student_id}/surveys

    // GET student info /{student_id}/surveys/{survey_id}/answers
    // TODO Create a proper subresource.
    @GET
    @Path("/{student_id}/surveys/{survey_id}/answers")
    public List<StudentThemeAverage> getStudentThemeAverage(
            @PathParam("class_id") int class_id,
            @PathParam("survey_id") int survey_id,
            @PathParam("student_id") int student_id) {
        return service.getStudentThemeAverage(survey_id, student_id);

    }
    
    @GET
    @Path("/{student_id}")
    public Student getStudent(
            @PathParam("class_id") int class_id,
            @PathParam("student_id") int student_id) {
        System.out.println("fetching student");
        return teacherStudentService.getStudent(student_id);
    }
    
    @GET
    //@Path("/{student_id}")
    public List<Student> getListOfStudents(@PathParam("class_id") int class_id) {
        System.out.println("fetching list of students from class");
        return teacherStudentService.getAllStudentsFromClass(class_id);
    }

}
