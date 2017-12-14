package org.dlearn.helsinki.skeleton.resource;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.dlearn.helsinki.skeleton.model.ClassWithAllGroups;
import org.dlearn.helsinki.skeleton.model.Classes;
import org.dlearn.helsinki.skeleton.service.ClassService;
import org.dlearn.helsinki.skeleton.model.ListClassThemeAverage;
import org.dlearn.helsinki.skeleton.service.ProgressionService;

public class TeacherClassResource {
    private final ClassService classService = new ClassService();
    private final ProgressionService progression = new ProgressionService();

    /**
     * request teachers/{teacher_id}/classes
     * @param teacher_id
     * @return the teacher's classes based on the teacher_id.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<ClassWithAllGroups> getClasses(
            @PathParam("teacher_id") int teacher_id) {
        System.out.println("producing list of classes with groups");
        return classService.getAllClassesWithGroups(teacher_id); // returns only open groups
    }

    /**
     * Adds new Class
     * @param teacher_id
     * @param teacher_class 
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public void addNewClass(@PathParam("teacher_id") int teacher_id,
            Classes teacher_class) {
        System.out.println("posting the new class " + teacher_class.getName()
                + " : " + teacher_class.getName_fi() + " to teacher "
                + teacher_id);
        teacher_class.setTeacher_id(teacher_id);
        classService.addClassToTeacher(teacher_class);
    }

    /**
     * Get avg progression for a class
     * @param class_id
     * @param amount, amount of items show.
     * @return 
     */
    @GET
    @Path("/{class_id}/progression/{amount}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ListClassThemeAverage> getClassAverage(
            @PathParam("class_id") int class_id,
            @PathParam("amount") int amount) {
        return progression.getClassProgression(class_id, amount);
    }

    /**
     * 
     * @param class_id
     * @return 
     */
    @Path("/{class_id}/surveys")
    public TeacherClassSurveyResource getSurveyResource(
            @PathParam("class_id") int class_id) {
        System.out.println("calling class surveys");
        return new TeacherClassSurveyResource();
    }

    /**
     * 
     * @param class_id
     * @return 
     */
    @Path("/{class_id}/groups")
    public TeacherClassGroupResource getGroupResource(
            @PathParam("class_id") int class_id) {
        System.out.println("calling class groups");
        return new TeacherClassGroupResource();
    }

    /**
     * 
     * @return 
     */
    @Path("/{class_id}/students")
    public TeacherClassStudentResource getClassStudent() {
        return new TeacherClassStudentResource();
    }
}
