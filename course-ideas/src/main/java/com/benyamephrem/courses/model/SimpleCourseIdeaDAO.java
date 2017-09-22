package com.benyamephrem.courses.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

//Abstracts away data operations and specifics of how database works
//but DAO should be persistent and not in memory like this

public class SimpleCourseIdeaDAO implements CourseIdeaDAO{

    private List<CourseIdea> ideas;

    public SimpleCourseIdeaDAO() {
        ideas = new ArrayList<>();
    }

    @Override
    public boolean add(CourseIdea idea) {
        return ideas.add(idea);
    }

    @Override
    public List<CourseIdea> findAll() {
        return new ArrayList<>(ideas);
    }

    @Override
    public CourseIdea findBySlug(String slug) {
        return ideas.stream().filter(idea -> idea.getSlug().equals(slug))
                .findFirst().orElseThrow(NotFoundException::new);

        //We will try to find the slug, if we can't we throw the NotFoundException
        //We make a stream of ideas and filter through for the one slug we want and
        //find the first match of it...OR ELSE THROW the exception
    }
}
